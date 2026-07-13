#Requires -Version 5.1

[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'Medium')]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8082',
    [string]$Username = $env:ADMIN_USERNAME,
    [string]$Password = $env:ADMIN_PASSWORD,
    [switch]$Force,
    [ValidateRange(1, 300)]
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

function Get-WebExceptionMessage {
    param(
        [Parameter(Mandatory = $true)]
        [System.Net.WebException]$Exception
    )

    $status = $null
    $body = ''
    $response = $Exception.Response
    if ($null -ne $response) {
        try {
            if ($response -is [System.Net.HttpWebResponse]) {
                $status = [int]$response.StatusCode
            }
            $stream = $response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8)
                try {
                    $body = $reader.ReadToEnd()
                }
                finally {
                    $reader.Dispose()
                }
            }
        }
        finally {
            $response.Close()
        }
    }

    if ([string]::IsNullOrWhiteSpace($body)) {
        $body = $Exception.Message
    }
    if ($null -ne $status) {
        return "HTTP $status - $body"
    }
    return $body
}

function ConvertFrom-ApiJson {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text,
        [Parameter(Mandatory = $true)]
        [string]$Operation
    )

    if ([string]::IsNullOrWhiteSpace($Text)) {
        throw "$Operation returned an empty response."
    }
    try {
        return ($Text | ConvertFrom-Json)
    }
    catch {
        throw "$Operation returned invalid JSON: $($_.Exception.Message)"
    }
}

function Invoke-JsonApiRequest {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('GET', 'POST', 'PUT')]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [string]$Token,
        [object]$Body,
        [Parameter(Mandatory = $true)]
        [string]$Operation
    )

    $request = [System.Net.HttpWebRequest]::Create($Uri)
    $request.Method = $Method
    $request.Accept = 'application/json'
    $request.Timeout = $TimeoutSeconds * 1000
    $request.ReadWriteTimeout = $TimeoutSeconds * 1000
    $request.UserAgent = 'medicine-sample-image-importer/1.0'
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $request.Headers['Authorization'] = $Token
    }

    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 10 -Compress
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $request.ContentType = 'application/json; charset=utf-8'
        $request.ContentLength = $bytes.Length
        $requestStream = $request.GetRequestStream()
        try {
            $requestStream.Write($bytes, 0, $bytes.Length)
        }
        finally {
            $requestStream.Dispose()
        }
    }
    elseif ($Method -ne 'GET') {
        $request.ContentLength = 0
    }

    try {
        $response = [System.Net.HttpWebResponse]$request.GetResponse()
        try {
            $reader = [System.IO.StreamReader]::new(
                $response.GetResponseStream(),
                [System.Text.Encoding]::UTF8
            )
            try {
                $text = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
        }
        finally {
            $response.Close()
        }
    }
    catch [System.Net.WebException] {
        $detail = Get-WebExceptionMessage -Exception $_.Exception
        throw "$Operation failed at ${Uri}: $detail"
    }

    return ConvertFrom-ApiJson -Text $text -Operation $Operation
}

function Invoke-ImageUpload {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [Parameter(Mandatory = $true)]
        [string]$Token,
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(Mandatory = $true)]
        [string]$Operation
    )

    $boundary = '----------------medicine-' + [guid]::NewGuid().ToString('N')
    $fileName = [System.IO.Path]::GetFileName($FilePath)
    $header = "--$boundary`r`n" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"`r`n" +
        "Content-Type: image/jpeg`r`n`r`n"
    $footer = "`r`n--$boundary--`r`n"
    $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($header)
    $footerBytes = [System.Text.Encoding]::ASCII.GetBytes($footer)
    $fileInfo = Get-Item -LiteralPath $FilePath

    $request = [System.Net.HttpWebRequest]::Create($Uri)
    $request.Method = 'POST'
    $request.Accept = 'application/json'
    $request.ContentType = "multipart/form-data; boundary=$boundary"
    $request.ContentLength = $headerBytes.Length + $fileInfo.Length + $footerBytes.Length
    $request.Timeout = $TimeoutSeconds * 1000
    $request.ReadWriteTimeout = $TimeoutSeconds * 1000
    $request.UserAgent = 'medicine-sample-image-importer/1.0'
    $request.Headers['Authorization'] = $Token
    $request.ServicePoint.Expect100Continue = $false

    try {
        $requestStream = $request.GetRequestStream()
        try {
            $requestStream.Write($headerBytes, 0, $headerBytes.Length)
            $fileStream = [System.IO.File]::OpenRead($fileInfo.FullName)
            try {
                $buffer = New-Object byte[] 65536
                while (($read = $fileStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
                    $requestStream.Write($buffer, 0, $read)
                }
            }
            finally {
                $fileStream.Dispose()
            }
            $requestStream.Write($footerBytes, 0, $footerBytes.Length)
        }
        finally {
            $requestStream.Dispose()
        }

        $response = [System.Net.HttpWebResponse]$request.GetResponse()
        try {
            $reader = [System.IO.StreamReader]::new(
                $response.GetResponseStream(),
                [System.Text.Encoding]::UTF8
            )
            try {
                $text = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
        }
        finally {
            $response.Close()
        }
    }
    catch [System.Net.WebException] {
        $detail = Get-WebExceptionMessage -Exception $_.Exception
        throw "$Operation failed at ${Uri}: $detail"
    }

    return ConvertFrom-ApiJson -Text $text -Operation $Operation
}

function Assert-ApiSuccess {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Response,
        [Parameter(Mandatory = $true)]
        [string]$Operation
    )

    if ($null -eq $Response.code -or [int]$Response.code -ne 20000) {
        $code = if ($null -eq $Response.code) { '<missing>' } else { [string]$Response.code }
        $message = if ([string]::IsNullOrWhiteSpace([string]$Response.message)) {
            '<no message>'
        }
        else {
            [string]$Response.message
        }
        throw "$Operation failed with API code ${code}: $message"
    }
}

function Get-ExactDrug {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ApiBase,
        [Parameter(Mandatory = $true)]
        [string]$Token,
        [Parameter(Mandatory = $true)]
        [string]$DrugName
    )

    $encodedName = [System.Uri]::EscapeDataString($DrugName)
    $response = Invoke-JsonApiRequest -Method GET `
        -Uri "$ApiBase/drugs/1/200?name=$encodedName" `
        -Token $Token `
        -Operation "Query drug '$DrugName'"
    Assert-ApiSuccess -Response $response -Operation "Query drug '$DrugName'"

    $rows = @($response.data.drugPageInfo.list)
    $matches = @($rows | Where-Object {
        [string]::Equals([string]$_.drugName, $DrugName, [System.StringComparison]::Ordinal)
    })
    if ($matches.Count -eq 0) {
        throw "No exact drug match was found for '$DrugName'."
    }
    if ($matches.Count -gt 1) {
        throw "Multiple exact drug matches were found for '$DrugName'; refusing an ambiguous update."
    }
    return $matches[0]
}

function Resolve-PublicUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value,
        [Parameter(Mandatory = $true)]
        [string]$ServiceBase
    )

    $absolute = $null
    if ([System.Uri]::TryCreate($Value, [System.UriKind]::Absolute, [ref]$absolute)) {
        return $absolute.AbsoluteUri
    }
    if ($Value.StartsWith('/')) {
        $serviceUri = [System.Uri]$ServiceBase
        return $serviceUri.GetLeftPart([System.UriPartial]::Authority) + $Value
    }
    return "$ServiceBase/$Value"
}

function Test-HttpGet200 {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri
    )

    try {
        $request = [System.Net.HttpWebRequest]::Create($Uri)
        $request.Method = 'GET'
        $request.AllowAutoRedirect = $true
        $request.MaximumAutomaticRedirections = 5
        $request.Timeout = $TimeoutSeconds * 1000
        $request.ReadWriteTimeout = $TimeoutSeconds * 1000
        $request.UserAgent = 'medicine-sample-image-importer/1.0'
        $response = [System.Net.HttpWebResponse]$request.GetResponse()
        try {
            return ([int]$response.StatusCode -eq 200)
        }
        finally {
            $response.Close()
        }
    }
    catch {
        Write-Verbose "GET verification failed for '$Uri': $($_.Exception.Message)"
        return $false
    }
}

function Get-SaleIds {
    param([object]$Drug)

    return @(
        @($Drug.drugSales) |
            Where-Object { $null -ne $_.saleId } |
            ForEach-Object { [long]$_.saleId } |
            Sort-Object -Unique
    )
}

function Test-LongSetEqual {
    param(
        [object[]]$Left,
        [object[]]$Right
    )

    $leftText = (@($Left | ForEach-Object { [long]$_ } | Sort-Object -Unique) -join ',')
    $rightText = (@($Right | ForEach-Object { [long]$_ } | Sort-Object -Unique) -join ',')
    return $leftText -eq $rightText
}

function Test-NullableStringEqual {
    param(
        [object]$Left,
        [object]$Right
    )

    if ($null -eq $Left -and $null -eq $Right) {
        return $true
    }
    if ($null -eq $Left -or $null -eq $Right) {
        return $false
    }
    return [string]::Equals(
        [string]$Left,
        [string]$Right,
        [System.StringComparison]::Ordinal
    )
}

function Assert-JpegAsset {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Sample image is missing: $Path"
    }
    $file = Get-Item -LiteralPath $Path
    if ($file.Length -le 0) {
        throw "Sample image is empty: $Path"
    }
    if ($file.Length -ge (2 * 1024 * 1024)) {
        throw "Sample image must be smaller than 2 MiB for the existing UI: $Path"
    }

    $stream = [System.IO.File]::OpenRead($file.FullName)
    try {
        if ($stream.Length -lt 3 -or
            $stream.ReadByte() -ne 0xFF -or
            $stream.ReadByte() -ne 0xD8 -or
            $stream.ReadByte() -ne 0xFF) {
            throw "Sample image does not have a JPEG signature: $Path"
        }
    }
    finally {
        $stream.Dispose()
    }
    return $file.FullName
}

$manifestJson = @'
[
  {
    "drugName": "\u590d\u65b9\u611f\u5192\u7075\u9897\u7c92(\u53cc\u8681)",
    "fileName": "medicine-assorted.jpg"
  },
  {
    "drugName": "\u8fde\u82b1\u6e05\u761f\u80f6\u56ca (\u4ee5\u5cad)",
    "fileName": "medicine-capsule.jpg"
  },
  {
    "drugName": "\u5e03\u6d1b\u82ac\u6df7\u60ac\u6db2 (\u7f8e\u6797)",
    "fileName": "medicine-bottle.jpg"
  },
  {
    "drugName": "\u590d\u65b9\u5bf9\u4e59\u9170\u6c28\u57fa\u915a\u7247(\u6563\u5229\u75db)",
    "fileName": "medicine-blister.jpg"
  },
  {
    "drugName": "\u590d\u65b9\u6c28\u915a\u70f7\u80fa\u7247(\u611f\u53f9\u53f7)",
    "fileName": "medicine-colorful.jpg"
  },
  {
    "drugName": "\u6c28\u5496\u9ec4\u654f\u80f6\u56ca(\u79be\u7a57\u901f\u6821)",
    "fileName": "medicine-hand.jpg"
  }
]
'@

$token = $null
$failed = $false
$failureMessage = $null

try {
    if ([string]::IsNullOrWhiteSpace($Username)) {
        throw 'Username is required. Pass -Username or set ADMIN_USERNAME.'
    }
    if ([string]::IsNullOrEmpty($Password)) {
        throw 'Password is required. Pass -Password or set ADMIN_PASSWORD.'
    }

    $baseText = $BaseUrl.Trim().TrimEnd('/')
    if ([string]::IsNullOrWhiteSpace($baseText)) {
        throw 'BaseUrl must not be empty.'
    }
    if ($baseText.EndsWith('/api', [System.StringComparison]::OrdinalIgnoreCase)) {
        $serviceBase = $baseText.Substring(0, $baseText.Length - 4).TrimEnd('/')
    }
    else {
        $serviceBase = $baseText
    }

    $serviceUri = $null
    if (-not [System.Uri]::TryCreate(
        $serviceBase,
        [System.UriKind]::Absolute,
        [ref]$serviceUri
    )) {
        throw "BaseUrl must be an absolute HTTP(S) URL: $BaseUrl"
    }
    if ($serviceUri.Scheme -ne 'http' -and $serviceUri.Scheme -ne 'https') {
        throw "BaseUrl must use HTTP or HTTPS: $BaseUrl"
    }
    $serviceBase = $serviceBase.TrimEnd('/')
    $apiBase = "$serviceBase/api"

    $manifest = $manifestJson | ConvertFrom-Json
    if ($manifest.Count -ne 6) {
        throw "The embedded manifest must contain exactly six entries; found $($manifest.Count)."
    }

    $assetRoot = [System.IO.Path]::GetFullPath(
        (Join-Path $PSScriptRoot '..\src\assets\medical-samples')
    )
    $assetPaths = @{}
    foreach ($entry in $manifest) {
        $candidate = [System.IO.Path]::GetFullPath(
            (Join-Path $assetRoot ([string]$entry.fileName))
        )
        if (-not $candidate.StartsWith($assetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Manifest path escapes the sample image directory: $($entry.fileName)"
        }
        $assetPaths[[string]$entry.fileName] = Assert-JpegAsset -Path $candidate
    }

    Write-Host "Logging in to $apiBase ..."
    $login = Invoke-JsonApiRequest -Method POST `
        -Uri "$apiBase/login" `
        -Body ([ordered]@{ username = $Username.Trim(); password = $Password }) `
        -Operation 'Administrator login'
    Assert-ApiSuccess -Response $login -Operation 'Administrator login'
    $token = [string]$login.data.token
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw 'Administrator login succeeded but did not return data.token.'
    }

    $updated = 0
    $skipped = 0
    $planned = 0

    foreach ($entry in $manifest) {
        $drugName = [string]$entry.drugName
        $fileName = [string]$entry.fileName
        $filePath = [string]$assetPaths[$fileName]
        $drug = Get-ExactDrug -ApiBase $apiBase -Token $token -DrugName $drugName
        $drugId = [long]$drug.drugId
        $currentImage = [string]$drug.drugImg

        if (-not $Force -and -not [string]::IsNullOrWhiteSpace($currentImage)) {
            $currentImageUrl = Resolve-PublicUrl -Value $currentImage -ServiceBase $serviceBase
            if (Test-HttpGet200 -Uri $currentImageUrl) {
                Write-Host "[SKIP] $drugName already has a reachable image: $currentImageUrl"
                $skipped++
                continue
            }
        }

        $action = "Upload '$fileName' and update drug ID $drugId"
        if (-not $PSCmdlet.ShouldProcess($drugName, $action)) {
            Write-Host "[PLAN] $drugName <- $fileName"
            $planned++
            continue
        }

        Write-Host "[UPLOAD] $drugName <- $fileName"
        $upload = Invoke-ImageUpload `
            -Uri "$apiBase/base/upload" `
            -Token $token `
            -FilePath $filePath `
            -Operation "Upload sample image for '$drugName'"
        Assert-ApiSuccess -Response $upload -Operation "Upload sample image for '$drugName'"
        $newImage = [string]$upload.data.url
        if ([string]::IsNullOrWhiteSpace($newImage)) {
            throw "Upload for '$drugName' succeeded but did not return data.url."
        }

        $saleIds = @(Get-SaleIds -Drug $drug)
        $payload = [ordered]@{
            drugName = $drug.drugName
            drugInfo = $drug.drugInfo
            drugEffect = $drug.drugEffect
            drugImg = $newImage
            drugPublisher = $drug.drugPublisher
            saleIds = [object[]]$saleIds
        }

        try {
            $update = Invoke-JsonApiRequest -Method PUT `
                -Uri "$apiBase/drugs/$drugId" `
                -Token $token `
                -Body $payload `
                -Operation "Update drug '$drugName'"
            Assert-ApiSuccess -Response $update -Operation "Update drug '$drugName'"
        }
        catch {
            throw "Image '$newImage' was uploaded, but drug '$drugName' was not updated. " +
                "The uploaded file may be orphaned. $($_.Exception.Message)"
        }

        $verificationUrl = Resolve-PublicUrl -Value $newImage -ServiceBase $serviceBase
        if (-not (Test-HttpGet200 -Uri $verificationUrl)) {
            throw "Drug '$drugName' was updated, but GET verification did not return HTTP 200: " +
                $verificationUrl
        }

        $verified = Get-ExactDrug -ApiBase $apiBase -Token $token -DrugName $drugName
        if ([long]$verified.drugId -ne $drugId) {
            throw "Verification returned an unexpected drug ID for '$drugName'."
        }
        if (-not [string]::Equals(
            [string]$verified.drugImg,
            $newImage,
            [System.StringComparison]::Ordinal
        )) {
            throw "Verification found a different image URL for '$drugName'."
        }
        if (-not (Test-NullableStringEqual -Left $drug.drugInfo -Right $verified.drugInfo) -or
            -not (Test-NullableStringEqual -Left $drug.drugEffect -Right $verified.drugEffect) -or
            -not (Test-NullableStringEqual -Left $drug.drugPublisher -Right $verified.drugPublisher)) {
            throw "Verification found changed non-image fields for '$drugName'."
        }
        $verifiedSaleIds = @(Get-SaleIds -Drug $verified)
        if (-not (Test-LongSetEqual -Left $saleIds -Right $verifiedSaleIds)) {
            throw "Verification found changed saleIds for '$drugName'."
        }

        Write-Host "[OK] $drugName -> $newImage"
        $updated++
    }

    Write-Host "Completed. Updated: $updated; skipped: $skipped; planned only: $planned."
}
catch {
    $failed = $true
    $failureMessage = $_.Exception.Message
}
finally {
    if (-not [string]::IsNullOrWhiteSpace($token)) {
        try {
            $logout = Invoke-JsonApiRequest -Method POST `
                -Uri "$apiBase/logout" `
                -Token $token `
                -Operation 'Logout'
            Assert-ApiSuccess -Response $logout -Operation 'Logout'
        }
        catch {
            Write-Warning "Logout cleanup failed: $($_.Exception.Message)"
        }
    }
}

if ($failed) {
    Write-Error "Sample drug image import failed: $failureMessage" -ErrorAction Continue
    exit 1
}
exit 0
