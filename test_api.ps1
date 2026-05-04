# Simple test script for the Bookstore API
# Requires the server to be running (.\run.ps1)

$BaseUrl = "http://localhost:8080"
$SessionId = "user-123"

Write-Host "`n--- 1. Health Check ---" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get

Write-Host "`n--- 2. List Books ---" -ForegroundColor Cyan
$books = Invoke-RestMethod -Uri "$BaseUrl/api/books" -Method Get
$books | Format-Table

Write-Host "`n--- 3. Add Item to Cart ---" -ForegroundColor Cyan
$cartBody = @{
    bookId = 1
    quantity = 2
} | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/api/cart/$SessionId/items" -Method Post -Body $cartBody -ContentType "application/json"

Write-Host "`n--- 4. View Cart ---" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$BaseUrl/api/cart/$SessionId" -Method Get

Write-Host "`n--- 5. Checkout ---" -ForegroundColor Cyan
$order = Invoke-RestMethod -Uri "$BaseUrl/api/cart/$SessionId/checkout" -Method Post
Write-Host "Order Placed! Total: $($order.totalAmount)" -ForegroundColor Green

Write-Host "`n--- 6. View Orders ---" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$BaseUrl/api/orders/$SessionId" -Method Get
