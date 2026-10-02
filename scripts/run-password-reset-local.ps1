# Chạy RMS để thử Forgot/Reset Password với SMTP AUTH + STARTTLS (mặc định Gmail).
# Không ghi SMTP App Password hoặc signing secret vào file hay command line.
[CmdletBinding()]
param(
    [ValidateRange(1, 65535)][int]$Port = 8082,
    [ValidateNotNullOrEmpty()][string]$SmtpHost = 'smtp.gmail.com',
    [ValidateRange(1, 65535)][int]$SmtpPort = 587
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'src/main/resources/application.properties'))) {
    throw 'Thiếu application.properties local để kết nối SQL Server.'
}
$maven = if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) {
    'mvn.cmd'
} elseif (Test-Path -LiteralPath (Join-Path $projectRoot 'mvnw.cmd')) {
    Join-Path $projectRoot 'mvnw.cmd'
} else {
    throw 'Không tìm thấy mvn.cmd hoặc mvnw.cmd.'
}

$mailboxInput = Read-Host 'Email SMTP/test mailbox bạn kiểm soát'
if ([string]::IsNullOrWhiteSpace($mailboxInput)) {
    throw 'Email SMTP không được để trống.'
}
try {
    $mailbox = ([System.Net.Mail.MailAddress]::new($mailboxInput.Trim())).Address
} catch {
    throw 'Email SMTP không hợp lệ.'
}

$securePassword = Read-Host 'SMTP App Password (không hiển thị)' -AsSecureString
$pointer = [IntPtr]::Zero
$smtpPassword = $null
try {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $smtpPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
} finally {
    if ($pointer -ne [IntPtr]::Zero) {`
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}
if ([string]::IsNullOrWhiteSpace($smtpPassword)) {
    throw 'SMTP App Password không được để trống.'
}

$previousSecret = [Environment]::GetEnvironmentVariable('APP_PASSWORD_RESET_SECRET', 'Process')
if (-not [string]::IsNullOrEmpty($previousSecret) -and
    [Text.Encoding]::UTF8.GetByteCount($previousSecret) -lt 32) {
    throw 'APP_PASSWORD_RESET_SECRET hiện tại ngắn hơn 32 byte.'
}
if ([string]::IsNullOrEmpty($previousSecret)) {
    $randomBytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($randomBytes)
        $signingSecret = [Convert]::ToBase64String($randomBytes)
    } finally {
        $rng.Dispose()
        [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    }
} else {
    $signingSecret = $previousSecret
}

$settings = [ordered]@{
    SPRING_MAIL_HOST = $SmtpHost
    SPRING_MAIL_PORT = [string]$SmtpPort
    SPRING_MAIL_USERNAME = $mailbox
    SPRING_MAIL_PASSWORD = $smtpPassword
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = 'true'
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = 'true'
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED = 'true'
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT = '10000'
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT = '10000'
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT = '10000'
    APP_MAIL_FROM = $mailbox
    APP_PUBLIC_BASE_URL = "http://localhost:$Port"
    APP_PASSWORD_RESET_SECRET = $signingSecret
    SERVER_PORT = [string]$Port
}
$previous = @{}
foreach ($name in $settings.Keys) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

Push-Location -LiteralPath $projectRoot
try {
    foreach ($name in $settings.Keys) {
        [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
    }
    Write-Host "Sau khi thấy 'Started RmsApplication', mở http://localhost:$Port/forgot-password"
    Write-Host 'Giữ cửa sổ này mở trong suốt bài test. Link cũ mất hiệu lực nếu signing secret thay đổi khi chạy lại.'
    & $maven spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Maven kết thúc với mã $LASTEXITCODE; xem lỗi startup ở trên."
    }
    Write-Warning "Maven đã dừng. Kiểm tra log 'Started RmsApplication' và GET /forgot-password; Maven có thể trả mã 0 dù Tomcat khởi động lỗi."
} finally {
    foreach ($name in $settings.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
    $smtpPassword = $null
    $signingSecret = $null
    Pop-Location
}
