#!/usr/bin/env python3
"""Measure a per-line vehicle speed baseline for Massilia's live mode.

Replays the app's behaviour against the RTM interactive-map webservice for N
minutes: polls /Vehicles/LastUpdate, fetches all lines on each feed tick,
projects consecutive positions of every vehicle onto the line traces from
lines.bin (same curvilinear-abscissa math as VehiclePathInterpolator), and
aggregates:
  - speedMps: mean commercial speed per line (includes dwell time at stops);
  - signs: per feed Direction ("1"/"2") and per trace path index, the sign of
    abscissa progression (+1/-1) — lets the app dead-reckon new vehicles in
    the right direction from the very first tick.

Usage:
    python tools/build_vehicle_speed_baseline.py --minutes 10
Then paste the generated JSON into config.json under
transport.vehicleSpeedBaseline (or use --print-config).
"""

import argparse
import json
import math
import ssl
import struct
import sys
import time
import urllib.request
from collections import defaultdict
from pathlib import Path

BASE = "https://carte-interactive.rtm.fr/WS"

# The dev machine sits behind a TLS-intercepting proxy whose CA fails Python's
# verification (same reason new Gradle deps can't download). Measurement tool
# only — skip verification.
SSL_CTX = ssl._create_unverified_context()
MAX_SNAP_METERS = 120.0
MAX_SPEED_MPS = 35.0  # > 126 km/h between ticks = glitch
METERS_PER_DEG_LAT = 111132.0


# --------------------------------------------------------------------------
# RLN2 parsing (same layout as RtmLinesParser.kt)
# --------------------------------------------------------------------------

class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def u16(self):
        (v,) = struct.unpack_from("<H", self.data, self.pos)
        self.pos += 2
        return v

    def u32(self):
        (v,) = struct.unpack_from("<I", self.data, self.pos)
        self.pos += 4
        return v

    def i32(self):
        (v,) = struct.unpack_from("<i", self.data, self.pos)
        self.pos += 4
        return v

    def string(self):
        n = self.u16()
        s = self.data[self.pos:self.pos + n].decode("utf-8")
        self.pos += n
        return s


def parse_lines_bin(path: Path):
    """-> {line_name_upper: [path0_points, path1_points, ...]} with points [lon, lat]."""
    r = Reader(path.read_bytes())
    magic = r.data[:4]
    r.pos = 4
    if magic != b"RLN2":
        sys.exit(f"Bad magic in {path}: {magic!r}")
    _version = r.u16()
    scale = r.u32()
    line_count = r.u32()
    lines = {}
    for _ in range(line_count):
        _line_id = r.u32()
        name = r.string()
        _color = r.string()
        _text_color = r.string()
        _transport_type = r.u16()
        path_count = r.u16()
        paths = []
        for _ in range(path_count):
            _direction_id = r.u16()
            n = r.u32()
            xs, ys = [0] * n, [0] * n
            acc = 0
            for i in range(n):
                acc += r.i32()
                xs[i] = acc
            acc = 0
            for i in range(n):
                acc += r.i32()
                ys[i] = acc
            paths.append([[x / scale, y / scale] for x, y in zip(xs, ys)])
        lines[name.strip().upper()] = paths
    return lines


# --------------------------------------------------------------------------
# Polyline projection (same math as VehiclePathInterpolator.kt)
# --------------------------------------------------------------------------

class Polyline:
    def __init__(self, points):
        lat_ref = points[0][1]
        self.m_lon = METERS_PER_DEG_LAT * math.cos(math.radians(lat_ref))
        self.xs = [p[0] * self.m_lon for p in points]
        self.ys = [p[1] * METERS_PER_DEG_LAT for p in points]
        self.cum = [0.0] * len(points)
        for i in range(1, len(points)):
            dx = self.xs[i] - self.xs[i - 1]
            dy = self.ys[i] - self.ys[i - 1]
            self.cum[i] = self.cum[i - 1] + math.hypot(dx, dy)

    def project(self, lon, lat):
        """-> (abscissa, distance_m) of the closest point."""
        px, py = lon * self.m_lon, lat * METERS_PER_DEG_LAT
        best_d2, best_s = float("inf"), 0.0
        for i in range(len(self.xs) - 1):
            ax, ay = self.xs[i], self.ys[i]
            abx, aby = self.xs[i + 1] - ax, self.ys[i + 1] - ay
            l2 = abx * abx + aby * aby
            t = 0.0 if l2 == 0 else max(0.0, min(1.0, ((px - ax) * abx + (py - ay) * aby) / l2))
            dx, dy = px - (ax + abx * t), py - (ay + aby * t)
            d2 = dx * dx + dy * dy
            if d2 < best_d2:
                best_d2, best_s = d2, self.cum[i] + math.sqrt(l2) * t
        return best_s, math.sqrt(best_d2)

    def project_near(self, lon, lat, ref_s, max_m):
        """Abscissa of the candidate within max_m nearest IN ABSCISSA to ref_s."""
        px, py = lon * self.m_lon, lat * METERS_PER_DEG_LAT
        max_d2 = max_m * max_m
        best_s, best_delta = None, float("inf")
        for i in range(len(self.xs) - 1):
            ax, ay = self.xs[i], self.ys[i]
            abx, aby = self.xs[i + 1] - ax, self.ys[i + 1] - ay
            l2 = abx * abx + aby * aby
            t = 0.0 if l2 == 0 else max(0.0, min(1.0, ((px - ax) * abx + (py - ay) * aby) / l2))
            dx, dy = px - (ax + abx * t), py - (ay + aby * t)
            if dx * dx + dy * dy > max_d2:
                continue
            s = self.cum[i] + math.sqrt(l2) * t
            if abs(s - ref_s) < best_delta:
                best_delta, best_s = abs(s - ref_s), s
        return best_s


# --------------------------------------------------------------------------
# Feed access
# --------------------------------------------------------------------------

def fetch(url):
    with urllib.request.urlopen(url, timeout=30, context=SSL_CTX) as resp:
        return resp.read().decode("utf-8")


def fetch_vehicles(lines_param):
    body = fetch(f"{BASE}/siri/Vehicles?lines={lines_param}").strip()
    if body.startswith('"'):
        body = json.loads(body)  # double-encoded variant
    return json.loads(body)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--minutes", type=int, default=10)
    ap.add_argument("--lines-bin", default="app/src/commonMain/composeResources/files/raptor/lines.bin")
    ap.add_argument("--gtfs-routes", default=r"Z:\Android\Projects\GTFS_RTM\routes.txt")
    ap.add_argument("--out", default="tools/vehicle_speed_baseline.json")
    args = ap.parse_args()

    traces = parse_lines_bin(Path(args.lines_bin))
    polylines = {name: [Polyline(p) for p in paths if len(p) >= 2] for name, paths in traces.items()}

    # name -> RTM internal id from GTFS route_ids
    import csv
    id_by_name, name_by_id = {}, {}
    with open(args.gtfs_routes, encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            num = row["route_id"].removeprefix("RTM-")
            id_by_name[row["route_short_name"].strip().upper()] = num
            name_by_id[num] = row["route_short_name"].strip().upper()
    lines_param = ";".join(f"RTM:LNE:{i}" for i in sorted(name_by_id, key=lambda x: int(x)))

    speed_samples = defaultdict(list)              # line -> [m/s]
    sign_votes = defaultdict(lambda: defaultdict(int))  # (line, dir, path_idx) -> {sign: votes}
    prev_positions = {}                            # vehicle id -> (lon, lat, tick_ms)
    last_update_seen = None
    deadline = time.time() + args.minutes * 60
    ticks = 0

    print(f"Sampling for {args.minutes} min...", flush=True)
    while time.time() < deadline:
        try:
            last_update = fetch(f"{BASE}/siri/Vehicles/LastUpdate").strip().strip('"')
            if last_update != last_update_seen:
                tick_ms = int(last_update)
                vehicles = fetch_vehicles(lines_param)
                ticks += 1
                print(f"tick {ticks}: {len(vehicles)} vehicles", flush=True)
                for v in vehicles:
                    vid, line_id = v.get("Id"), (v.get("Line") or "").rpartition(":")[2]
                    lon, lat, direction = v.get("Longitude"), v.get("Latitude"), v.get("Direction")
                    line = name_by_id.get(line_id)
                    if not (vid and line and lon and lat):
                        continue
                    prev = prev_positions.get(vid)
                    prev_positions[vid] = (lon, lat, tick_ms)
                    if not prev or line not in polylines:
                        continue
                    plon, plat, ptick = prev
                    dt = (tick_ms - ptick) / 1000.0
                    if dt <= 5 or dt > 180:
                        continue
                    # anchored pair selection, same as the app
                    best = None  # (glide, path_idx, s0, s1)
                    for idx, poly in enumerate(polylines[line]):
                        s1, d1 = poly.project(lon, lat)
                        if d1 > MAX_SNAP_METERS:
                            continue
                        s0 = poly.project_near(plon, plat, s1, MAX_SNAP_METERS)
                        if s0 is None:
                            continue
                        glide = abs(s1 - s0)
                        if best is None or glide < best[0]:
                            best = (glide, idx, s0, s1)
                    if best is None:
                        continue
                    glide, idx, s0, s1 = best
                    speed = glide / dt
                    if speed > MAX_SPEED_MPS:
                        continue
                    speed_samples[line].append(speed)
                    if direction and glide > 30:  # only clear movements vote on the sign
                        sign_votes[(line, direction, idx)][1 if s1 > s0 else -1] += 1
                last_update_seen = last_update
        except Exception as e:  # transient network errors: keep sampling
            print(f"warn: {e}", flush=True)
        time.sleep(5)

    # Aggregate
    out = {}
    for line, samples in sorted(speed_samples.items()):
        if len(samples) < 5:
            continue
        mean_speed = sum(samples) / len(samples)
        signs = {}
        for (l, direction, idx), votes in sign_votes.items():
            if l != line:
                continue
            total = sum(votes.values())
            top_sign, top_votes = max(votes.items(), key=lambda kv: kv[1])
            if total >= 3 and top_votes / total >= 0.75:
                signs.setdefault(direction, {})[str(idx)] = top_sign
        entry = {"speedMps": round(mean_speed, 2), "samples": len(samples)}
        if signs:
            entry["signs"] = signs
        out[line] = entry

    result = {"measuredTicks": ticks, "lines": out}
    Path(args.out).write_text(json.dumps(result, indent=2, sort_keys=True), encoding="utf-8")
    print(f"\n{len(out)} lines with baseline -> {args.out}")
    for line, e in sorted(out.items()):
        print(f"  {line:6} {e['speedMps']:5.2f} m/s  ({e['samples']} samples, signs={e.get('signs', {})})")


if __name__ == "__main__":
    main()
