# modules/network/outputs.tf

output "subnet_id" {
  value = azurerm_subnet.this.id
}
