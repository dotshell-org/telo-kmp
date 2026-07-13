# Probe the RTM live vehicle positions webservice (the one backing the
# official interactive map, carte-interactive.rtm.fr — used by Telo's
# Live mode). Compatible Windows PowerShell 5.1 et PowerShell 7+.
#
# Usage:  .\tools\probe_rtm_siri.ps1                 # default line set
#         .\tools\probe_rtm_siri.ps1 B1,T1,M1,18     # commercial line names
#
# Line names are resolved to internal ids (RTM:LNE:<n>) via the local GTFS
# routes.txt; pass -GtfsRoutes to point elsewhere.
param(
    [string[]] $Lines = @("B1", "T1", "T2", "T3", "18", "35", "FERRY"),
    [string] $GtfsRoutes = "Z:\Android\Projects\GTFS_RTM\routes.txt"
)

$base = "https://carte-interactive.rtm.fr/WS"

# "B1,T1" arrives as one string when the script is invoked with powershell -File
$Lines = @($Lines | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' })

# PS 5.1: force TLS 1.2 and load HttpClient from .NET Framework
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Net.Http -ErrorAction SilentlyContinue

# Resolve commercial names -> internal ids
$idByName = @{}
if (Test-Path $GtfsRoutes) {
    Import-Csv $GtfsRoutes | ForEach-Object {
        $idByName[$_.route_short_name.ToUpper()] = ($_.route_id -replace '^RTM-', '')
    }
}
$ids = @()
foreach ($line in $Lines) {
    $key = $line.ToUpper()
    if ($idByName.ContainsKey($key)) { $ids += @{ name = $key; id = $idByName[$key] } }
    elseif ($key -match '^\d+$') { $ids += @{ name = $key; id = $key } }
    else { Write-Host ("{0,-6} SKIP: id inconnu (routes.txt introuvable ?)" -f $line) }
}
if ($ids.Count -eq 0) { Write-Host "Aucune ligne résolue."; exit 1 }

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(30)

try {
    $lastUpdate = $client.GetStringAsync("$base/siri/Vehicles/LastUpdate").GetAwaiter().GetResult()
    $lastUpdateMs = 0
    if ([long]::TryParse($lastUpdate.Trim('"'), [ref]$lastUpdateMs)) {
        $dt = [DateTimeOffset]::FromUnixTimeMilliseconds($lastUpdateMs).ToLocalTime()
        Write-Host ("Feed LastUpdate : {0:yyyy-MM-dd HH:mm:ss} (locale)" -f $dt)
    } else {
        Write-Host "Feed LastUpdate : $lastUpdate"
    }

    $linesParam = ($ids | ForEach-Object { "RTM:LNE:$($_.id)" }) -join ";"
    $url = "$base/siri/Vehicles?lines=" + [uri]::EscapeDataString($linesParam)
    $body = $client.GetStringAsync($url).GetAwaiter().GetResult()

    # The WS sometimes double-encodes the array as a JSON string.
    $payload = $body.Trim()
    if ($payload.StartsWith('"')) { $payload = ConvertFrom-Json $payload }
    # PS 5.1 emits the parsed array as ONE object — force enumeration
    $vehicles = @((ConvertFrom-Json $payload) | ForEach-Object { $_ })

    Write-Host ("Total vehicles  : {0}" -f $vehicles.Count)
    foreach ($entry in $ids) {
        $count = @($vehicles | Where-Object { $_.Line -eq "RTM:LNE:$($entry.id)" }).Count
        Write-Host ("{0,-6} (RTM:LNE:{1,-5}) vehicles={2}" -f $entry.name, $entry.id, $count)
    }
} catch {
    $msg = if ($_.Exception.InnerException) { $_.Exception.InnerException.Message } else { $_.Exception.Message }
    Write-Host "ERROR: $msg"
} finally {
    $client.Dispose()
}
