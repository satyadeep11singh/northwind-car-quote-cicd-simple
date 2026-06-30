# modules/registry/main.tf
#
# Single ACR for the project. Basic SKU is sufficient here -- Standard/Premium
# add geo-replication and higher throughput this single-image, single-region
# pipeline doesn't need.

resource "azurerm_container_registry" "this" {
  name                = var.registry_name
  resource_group_name = var.resource_group_name
  location            = var.location
  sku                 = "Basic"
  admin_enabled       = false
  tags                = var.tags
}
