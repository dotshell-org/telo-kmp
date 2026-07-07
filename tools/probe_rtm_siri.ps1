# Probe the SIRI VehicleMonitoring feed of La Métropole Mobilité (RTM network).
# Usage:  .\tools\probe_rtm_siri.ps1              # probes a default set of lines
#         .\tools\probe_rtm_siri.ps1 B1,T1,M1,18  # probes specific lines
#
# The endpoint answers ONE line per request and is rate-limited to 10-20 req/min
# for everyone — keep the line list short.
param(
    [string[]] $Lines = @("B1", "T1", "T2", "18", "35")
)

$endpoint = "https://siri.lametropolemobilite.fr/RTM"
$requestorRef = "PAN-VM"

foreach ($line in $Lines) {
    $ts = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    $msgId = "MassiliaProbe:Message::$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()):LOC"
    $body = '<?xml version="1.0" encoding="UTF-8"?>' +
        '<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body>' +
        '<sw:GetVehicleMonitoring xmlns:sw="http://wsdl.siri.org.uk" xmlns:siri="http://www.siri.org.uk/siri">' +
        "<ServiceRequestInfo><siri:RequestTimestamp>$ts</siri:RequestTimestamp>" +
        "<siri:RequestorRef>$requestorRef</siri:RequestorRef>" +
        "<siri:MessageIdentifier>$msgId</siri:MessageIdentifier></ServiceRequestInfo>" +
        "<Request><siri:RequestTimestamp>$ts</siri:RequestTimestamp>" +
        "<siri:MessageIdentifier>$msgId</siri:MessageIdentifier>" +
        "<siri:LineRef>RTM:Line::$($line.ToUpper()):LOC</siri:LineRef></Request>" +
        '<RequestExtension/></sw:GetVehicleMonitoring></S:Body></S:Envelope>'

    try {
        $r = Invoke-WebRequest -Uri $endpoint -Method POST -Body $body -ContentType "text/xml; charset=utf-8" -TimeoutSec 60 -SkipHttpErrorCheck
        $count = ([regex]::Matches($r.Content, "<[^>]*VehicleActivity[ >]")).Count
        if ($r.StatusCode -eq 200) {
            Write-Host ("{0,-6} HTTP {1}  vehicles={2}" -f $line, $r.StatusCode, $count)
            if ($count -gt 0) {
                # Show the refs actually used by the hub — useful to confirm LineRef spelling
                $refs = [regex]::Matches($r.Content, "<[^>]*LineRef[^>]*>([^<]+)<") | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
                Write-Host ("       LineRefs seen: " + ($refs -join ", "))
            }
        } else {
            $fault = [regex]::Match($r.Content, "<faultstring>([^<]+)</faultstring>").Groups[1].Value
            Write-Host ("{0,-6} HTTP {1}  FAULT: {2}" -f $line, $r.StatusCode, $fault)
        }
    } catch {
        Write-Host ("{0,-6} ERROR: {1}" -f $line, $_.Exception.Message)
    }
    Start-Sleep -Milliseconds 800
}
