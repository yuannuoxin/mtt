$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8085"
$curl = "C:\Windows\System32\curl.exe"

function Send-It($path, $obj, $token) {
    $json = $obj | ConvertTo-Json -Depth 8
    $tmp = Join-Path $env:TEMP ("mtdtt_" + [guid]::NewGuid().ToString("N") + ".json")
    [IO.File]::WriteAllText($tmp, $json, (New-Object Text.UTF8Encoding $false))
    $h = @("--silent", "--show-error", "-X", "POST", "$base$path", "-H", "Content-Type: application/json")
    if ($token) { $h += @("-H", "Authorization: Bearer $token") }
    $h += @("--data-binary", "@$tmp")
    $r = & $curl @h 2>&1 | Out-String
    Remove-Item $tmp -ErrorAction SilentlyContinue
    return ($r | ConvertFrom-Json)
}

$un1 = "ttsmoke1_" + [guid]::NewGuid().ToString("N").Substring(0, 8)
$un2 = "ttsmoke2_" + [guid]::NewGuid().ToString("N").Substring(0, 8)

# 1+2 register
$r1 = Send-It "/auth/register" @{username = $un1; nickname = "SmokeOne"; email = "$un1@t.com"; password = "Smoke123456" } $null
$t1 = $r1.data.tokenValue; $id1 = $r1.data.user.id
Write-Output ("REG1 code={0} id={1}" -f $r1.code, $id1)
$r2 = Send-It "/auth/register" @{username = $un2; nickname = "SmokeTwo"; email = "$un2@t.com"; password = "Smoke123456" } $null
$t2 = $r2.data.tokenValue; $id2 = $r2.data.user.id
Write-Output ("REG2 code={0} id={1}" -f $r2.code, $id2)

# 3 login (is-concurrent=false 会顶掉旧 token，登录后用新 token)
$r3 = Send-It "/auth/passwordLogin" @{account = $un1; password = "Smoke123456" } $null
$t1 = $r3.data.tokenValue
Write-Output ("LOGIN code={0} newTokenLen={1}" -f $r3.code, $t1.Length)
# 4 userinfo
$r4 = Send-It "/auth/userInfo" @{} $t1
Write-Output ("USERINFO code={0} name={1}" -f $r4.code, $r4.data.username)
# 5 follow u2 -> u1
$r5 = Send-It "/user/follow" @{followeeId = $id1 } $t2
Write-Output ("FOLLOW code={0}" -f $r5.code)
# 6 create post with media
$r6 = Send-It "/post/create" @{type = "original"; content = "smoke test post hello world"; medias = @(@{mediaType = "image"; originalUrl = "https://ex.com/a.jpg"; thumbnailUrl = "https://ex.com/t.jpg"; width = 1200; height = 800 }) } $t1
$postId = $r6.data.id
Write-Output ("CREATE code={0} postId={1} media={2} img={3}" -f $r6.code, $postId, $r6.data.hasMedia, $r6.data.hasImage)
# 7 detail
$r7 = Send-It "/post/detail" @{postId = $postId } $t1
Write-Output ("DETAIL code={0} mediaCount={1} isMine={2}" -f $r7.code, $r7.data.medias.Count, $r7.data.isMine)
# 8 like
$r8 = Send-It "/post/like" @{postId = $postId } $t2
Write-Output ("LIKE code={0}" -f $r8.code)
# 9 timeline
$r9 = Send-It "/post/homeTimeline" @{pageNum = 1; pageSize = 10 } $t2
Write-Output ("TIMELINE code={0} total={1} firstLike={2} author={3}" -f $r9.code, $r9.data.total, $r9.data.list[0].isLiked, $r9.data.list[0].user.username)
# 10 notification
$r10 = Send-It "/notification/list" @{pageNum = 1; pageSize = 10 } $t1
Write-Output ("NOTIF code={0} total={1} firstType={2}" -f $r10.code, $r10.data.total, $r10.data.list[0].type)
# 11 search
$r11 = Send-It "/post/search" @{keyword = "smoke test"; pageNum = 1; pageSize = 10 } $t2
Write-Output ("SEARCH code={0} total={1}" -f $r11.code, $r11.data.total)
# 12 reply
$r12 = Send-It "/post/create" @{type = "reply"; content = "a reply"; referencedPostId = $postId } $t2
$replyId = $r12.data.id
Write-Output ("REPLY code={0} id={1} root={2}" -f $r12.code, $replyId, $r12.data.rootPostId)
# 13 bookmark
$r13 = Send-It "/post/bookmark" @{postId = $postId } $t2
Write-Output ("BOOKMARK code={0}" -f $r13.code)
$r14 = Send-It "/post/myBookmarks" @{pageNum = 1; pageSize = 10 } $t2
Write-Output ("MYBOOKMARKS code={0} total={1}" -f $r14.code, $r14.data.total)
# 15 profile (u1 sees u2)
$r15 = Send-It "/user/profile" @{userId = $id2 } $t1
Write-Output ("PROFILE code={0} following={1} isFollowed={2}" -f $r15.code, $r15.data.followingCount, $r15.data.isFollowed)
# unread count
$r16 = Send-It "/notification/unreadCount" @{} $t1
Write-Output ("UNREAD code={0} count={1}" -f $r16.code, $r16.data.count)
# user posts
$r17 = Send-It "/post/userPosts" @{userId = $id1; pageNum = 1; pageSize = 10 } $t1
Write-Output ("USERPOSTS code={0} total={1}" -f $r17.code, $r17.data.total)
# replies list
$r18 = Send-It "/post/replies" @{postId = $postId; pageNum = 1; pageSize = 10 } $t1
Write-Output ("REPLIES code={0} total={1}" -f $r18.code, $r18.data.total)
# followers list
$r19 = Send-It "/user/followers" @{userId = $id1; pageNum = 1; pageSize = 10 } $t2
Write-Output ("FOLLOWERS code={0} total={1}" -f $r19.code, $r19.data.total)

@{user1 = $id1; user2 = $id2; un = $un1; un2 = $un2; post = $postId; reply = $replyId; token1 = $t1 } | ConvertTo-Json | Out-File "C:\code\mtd\mtd-tt\doc\smoke_result.json" -Encoding UTF8
Write-Output "DONE"
