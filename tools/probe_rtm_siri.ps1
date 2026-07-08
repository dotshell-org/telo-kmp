# Probe the SIRI VehicleMonitoring feed of La Métropole Mobilité (RTM network).
# Usage:  .\tools\probe_rtm_siri.ps1              # probes a default set of lines
#         .\tools\probe_rtm_siri.ps1 B1,T1,M1,18  # probes specific lines
#
# Compatible Windows PowerShell 5.1 et PowerShell 7+.
# The endpoint answers ONE line per request and is rate-limited to 10-20 req/min
# for everyone — keep the line list short.
param(
    [string[]] $Lines = @("B1", "T1", "T2", "18", "35")
)

$endpoint = "https://siri.lametropolemobilite.fr/RTM"
$requestorRef = "PAN-VM"

# "B1,T1" arrives as one string when the script is invoked with powershell -File
$Lines = @($Lines | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' })

# PS 5.1: force TLS 1.2 and load HttpClient from .NET Framework
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Net.Http -ErrorAction SilentlyContinue

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)

try {
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
            $content = New-Object System.Net.Http.StringContent($body, [System.Text.Encoding]::UTF8, "text/xml")
            $resp = $client.PostAsync($endpoint, $content).GetAwaiter().GetResult()
            $status = [int]$resp.StatusCode
            $text = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()

            $count = ([regex]::Matches($text, "<[^>]*VehicleActivity[ >]")).Count
            if ($status -eq 200) {
                Write-Host ("{0,-6} HTTP {1}  vehicles={2}" -f $line, $status, $count)
                if ($count -gt 0) {
                    # Show the refs actually used by the hub — useful to confirm LineRef spelling
                    $refs = [regex]::Matches($text, "<[^>]*LineRef[^>]*>([^<]+)<") | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
                    Write-Host ("       LineRefs seen: " + ($refs -join ", "))
                }
            } else {
                $fault = [regex]::Match($text, "<faultstring>([^<]+)</faultstring>").Groups[1].Value
                Write-Host ("{0,-6} HTTP {1}  FAULT: {2}" -f $line, $status, $fault)
            }
        } catch {
            $msg = if ($_.Exception.InnerException) { $_.Exception.InnerException.Message } else { $_.Exception.Message }
            Write-Host ("{0,-6} ERROR: {1}" -f $line, $msg)
        }
        Start-Sleep -Milliseconds 800
    }
} finally {
    $client.Dispose()
}
