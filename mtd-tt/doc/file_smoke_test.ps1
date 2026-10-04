$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8085"
$curl = "C:\Windows\System32\curl.exe"

function Send-Json($path, $obj, $token) {
    $json = $obj | ConvertTo-Json -Depth 6
    $tmp = Join-Path $env:TEMP ("mtdtt_f_" + [guid]::NewGuid().ToString("N") + ".json")
    [IO.File]::WriteAllText($tmp, $json, (New-Object Text.UTF8Encoding $false))
    $h = @("--silent", "--show-error", "-X", "POST", "$base$path", "-H", "Content-Type: application/json")
    if ($token) { $h += @("-H", "Authorization: Bearer $token") }
    $h += @("--data-binary", "@$tmp")
    $r = & $curl @h 2>&1 | Out-String
    Remove-Item $tmp -ErrorAction SilentlyContinue
    return ($r | ConvertFrom-Json)
}

# 0) 注册拿 token
$un = "ttfile_" + [guid]::NewGuid().ToString("N").Substring(0, 8)
$r0 = Send-Json "/auth/register" @{username = $un; nickname = "FileTest"; email = "$un@t.com"; password = "Smoke123456" } $null
$t = $r0.data.tokenValue; $id = $r0.data.user.id
Write-Output ("REG code={0} id={1}" -f $r0.code, $id)

# 1) 创建本地测试文件
$testFile = Join-Path $env:TEMP ("mtdtt_upload_" + [guid]::NewGuid().ToString("N") + ".txt")
[IO.File]::WriteAllText($testFile, "hello minio upload test 12345", (New-Object Text.UTF8Encoding $false))
Write-Output ("TESTFILE size={0}" -f (Get-Item $testFile).Length)

# 2) 后端直传 /file/upload
$up = & $curl --silent --show-error -X POST "$base/file/upload" -H "Authorization: Bearer $t" -F "file=@$testFile;type=text/plain" 2>&1 | Out-String
$upJson = $up | ConvertFrom-Json
Write-Output ("UPLOAD code={0} url={1}" -f $upJson.code, $upJson.data)

# 3) 验证后端直传的文件可访问
$objUrl = $upJson.data
$code1 = & $curl --silent --show-error -o "$testFile.download" -w "%{http_code}" "$objUrl" 2>&1
$match1 = (Get-Content "$testFile.download" -Raw) -eq (Get-Content $testFile -Raw)
Write-Output ("UPLOAD_GET code={0} contentMatch={1}" -f $code1, $match1)

# 4) 预签名直传 /file/presignUpload
$r4 = Send-Json "/file/presignUpload" @{fileName = "presign_upload.txt"; contentType = "text/plain" } $t
$putUrl = $r4.data
Write-Output ("PRESIGN code={0} hasSignature={1}" -f $r4.code, ($putUrl -match "X-Amz-Signature"))

# 5) 用预签名 URL 直接 PUT 上传
$putCode = & $curl --silent --show-error -o NUL -w "%{http_code}" -X PUT --data-binary "@$testFile" "$putUrl" 2>&1
Write-Output ("PRESIGN_PUT code={0}" -f $putCode)

# 6) 验证预签名上传的文件可访问（去掉 query 即对象 URL）
$presignObjUrl = ($putUrl -split "\?")[0]
$code2 = & $curl --silent --show-error -o "$testFile.pdownload" -w "%{http_code}" "$presignObjUrl" 2>&1
$match2 = (Get-Content "$testFile.pdownload" -Raw) -eq (Get-Content $testFile -Raw)
Write-Output ("PRESIGN_GET code={0} contentMatch={1}" -f $code2, $match2)

@{userId = $id; url = $upJson.data; presignObjUrl = $presignObjUrl; testFile = $testFile } | ConvertTo-Json | Out-File "C:\code\mtd\mtd-tt\doc\file_smoke.json" -Encoding UTF8
Write-Output "DONE"
