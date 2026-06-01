param(
    [string]$BaseUrl = 'https://literaki.skiq.pl',
    [int]$MaxAttempts = 12
)

$ErrorActionPreference = 'Stop'

$seedWords = @('DOM', 'KOT', 'LAS', 'MAK', 'MAMA', 'RAK', 'ROK', 'SOK', 'TATA', 'TOR')

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Get-DictionaryWords {
    try {
        $content = Invoke-WebRequest -Uri "$BaseUrl/admin" -UseBasicParsing | Select-Object -ExpandProperty Content
        $matches = [regex]::Matches($content, '<tr>\s*<td>\d+</td>\s*<td>([^<]+)</td>\s*<td>pl</td>\s*</tr>')
        $words = @(
            $matches |
                ForEach-Object { $_.Groups[1].Value.Trim().ToUpperInvariant() } |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
                Select-Object -Unique
        )

        if ($words.Count -gt 0) {
            Write-Host ("Załadowano {0} słów z panelu admina." -f $words.Count) -ForegroundColor Cyan
            return $words
        }
    } catch {
        Write-Host "Nie udało się pobrać słownika z /admin, używam listy seedów z repozytorium." -ForegroundColor Yellow
    }

    return $seedWords
}

$allowedWords = Get-DictionaryWords

function New-Guest {
    param([string]$Username)

    Invoke-RestMethod `
        -Uri "$BaseUrl/api/v1/auth" `
        -Method Post `
        -ContentType 'application/json' `
        -Body (@{ username = $Username } | ConvertTo-Json)
}

function Invoke-AuthorizedPost {
    param(
        [string]$Uri,
        [string]$Token,
        [hashtable]$Body
    )

    Invoke-RestMethod `
        -Uri $Uri `
        -Method Post `
        -Headers @{ Authorization = "Bearer $Token" } `
        -ContentType 'application/json' `
        -Body ($Body | ConvertTo-Json -Depth 10)
}

function Get-AuthorizedJson {
    param(
        [string]$Uri,
        [string]$Token
    )

    Invoke-RestMethod `
        -Uri $Uri `
        -Headers @{ Authorization = "Bearer $Token" }
}

function Test-WordFromRack {
    param(
        [string[]]$Rack,
        [string]$Word
    )

    $letters = @{}
    foreach ($letter in $Rack) {
        if (-not $letters.ContainsKey($letter)) {
            $letters[$letter] = 0
        }
        $letters[$letter]++
    }

    foreach ($char in $Word.ToCharArray()) {
        $letter = [string]$char
        if (-not $letters.ContainsKey($letter) -or $letters[$letter] -le 0) {
            return $false
        }
        $letters[$letter]--
    }

    return $true
}

function Find-PlayableWord {
    param(
        [string[]]$Rack,
        [string[]]$AllowedWords
    )

    foreach ($word in ($AllowedWords | Sort-Object Length, @{ Expression = { $_ } })) {
        if (Test-WordFromRack -Rack $Rack -Word $word) {
            return $word
        }
    }

    return $null
}

function Build-Tiles {
    param(
        [string]$Word,
        [int]$StartX,
        [int]$StartY
    )

    $tiles = @()
    for ($index = 0; $index -lt $Word.Length; $index++) {
        $tiles += @{
            letter = $Word.Substring($index, 1)
            x = $StartX + $index
            y = $StartY
        }
    }
    return $tiles
}

$result = $null

for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
    Write-Host "[Attempt $attempt/$MaxAttempts] Tworzenie użytkowników testowych..."

    $playerA = New-Guest -Username ("ApiFlowA_{0}_{1}" -f $attempt, (Get-Random -Minimum 1000 -Maximum 9999))
    $playerB = New-Guest -Username ("ApiFlowB_{0}_{1}" -f $attempt, (Get-Random -Minimum 1000 -Maximum 9999))

    $createdGame = Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games" -Token $playerA.token -Body @{}
    $gameId = $createdGame.id

    [void](Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games/$gameId/join" -Token $playerB.token -Body @{})
    $startedGame = Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games/$gameId/start" -Token $playerA.token -Body @{}

    $stateA = Get-AuthorizedJson -Uri "$BaseUrl/api/v1/games/$gameId" -Token $playerA.token
    $stateB = Get-AuthorizedJson -Uri "$BaseUrl/api/v1/games/$gameId" -Token $playerB.token

    $currentToken = if ($stateA.current_turn_user_id -eq $playerA.user.id) { $playerA.token } else { $playerB.token }
    $otherToken = if ($currentToken -eq $playerA.token) { $playerB.token } else { $playerA.token }
    $currentState = if ($currentToken -eq $playerA.token) { $stateA } else { $stateB }

    $currentRack = ($currentState.players | Where-Object { $_.id -eq $currentState.current_turn_user_id }).rack
    $word = Find-PlayableWord -Rack $currentRack -AllowedWords $allowedWords

    if (-not $word) {
        Write-Host "Brak dozwolonego słowa na pierwszej turze, wykonuję PASS i sprawdzam drugiego gracza..."
        [void](Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games/$gameId/moves" -Token $currentToken -Body @{ move_type = 'pass' })

        $currentToken = $otherToken
        $currentState = Get-AuthorizedJson -Uri "$BaseUrl/api/v1/games/$gameId" -Token $currentToken
        $currentRack = ($currentState.players | Where-Object { $_.id -eq $currentState.current_turn_user_id }).rack
        $word = Find-PlayableWord -Rack $currentRack -AllowedWords $allowedWords
    }

    if (-not $word) {
        Write-Host "Nadal brak dozwolonego słowa w tej próbie. Przechodzę do kolejnej gry." -ForegroundColor Yellow
        continue
    }

    Write-Host "Znalezione słowo do zagrania: $word" -ForegroundColor Green

    $tiles = Build-Tiles -Word $word -StartX 7 -StartY 7
    [void](Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games/$gameId/moves" -Token $currentToken -Body @{ move_type = 'place_tiles'; tiles = $tiles })

    $afterPlaceA = Get-AuthorizedJson -Uri "$BaseUrl/api/v1/games/$gameId" -Token $playerA.token
    $passToken = if ($afterPlaceA.current_turn_user_id -eq $playerA.user.id) { $playerA.token } else { $playerB.token }
    [void](Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games/$gameId/moves" -Token $passToken -Body @{ move_type = 'pass' })

    $afterPassA = Get-AuthorizedJson -Uri "$BaseUrl/api/v1/games/$gameId" -Token $playerA.token
    $resignToken = if ($afterPassA.current_turn_user_id -eq $playerA.user.id) { $playerA.token } else { $playerB.token }
    [void](Invoke-AuthorizedPost -Uri "$BaseUrl/api/v1/games/$gameId/moves" -Token $resignToken -Body @{ move_type = 'resign' })

    $finalState = Get-AuthorizedJson -Uri "$BaseUrl/api/v1/games/$gameId" -Token $playerA.token

    $result = [pscustomobject]@{
        attempt = $attempt
        gameId = $gameId
        playerA = $playerA.user.username
        playerB = $playerB.user.username
        startedStatus = $startedGame.status
        playedWord = $word
        finalStatus = $finalState.status
        winnerId = $finalState.winner_id
        board = $finalState.board
        moves = @($finalState.moves | ForEach-Object {
            [pscustomobject]@{
                id = $_.id
                move_type = $_.move_type
                score = $_.score
                user_id = $_.user_id
            }
        })
    }

    break
}

if (-not $result) {
    throw "Nie udało się znaleźć racka z dozwolonym słowem w $MaxAttempts próbach."
}

$result | ConvertTo-Json -Depth 10

