# modules/network/main.tf
#
# One subnet for the AKS node pool, inside an existing VNet (the VNet is
# created once in environments/dev, not here). No NSG: AKS manages its own
# node-level security via Azure-managed rules, and adding a hand-written NSG
# on the AKS subnet risks silently blocking traffic AKS itself depends on.

resource "azurerm_subnet" "this" {
  name                 = var.subnet_name
  resource_group_name  = var.resource_group_name
  virtual_network_name = var.virtual_network_name
  address_prefixes     = [var.address_prefix]
}
