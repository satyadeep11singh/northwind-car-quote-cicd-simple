# environments/dev/main.tf
#
# Composes the registry, network, and aks modules into the Phase 7
# infrastructure: one ACR, one single-node AKS cluster, wired together via
# an explicit AcrPull role assignment on the cluster's kubelet identity (the
# Terraform-native equivalent of `az aks update --attach-acr`).

resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags
}

resource "azurerm_virtual_network" "main" {
  name                = "vnet-northwind-quote"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  address_space       = ["10.40.0.0/16"]
  tags                = var.tags
}

resource "random_id" "acr_suffix" {
  byte_length = 3
}

module "network_aks" {
  source = "../../modules/network"

  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  subnet_name           = "snet-aks"
  address_prefix        = "10.40.1.0/24"
}

module "registry" {
  source = "../../modules/registry"

  registry_name        = "acrnorthwindquote${random_id.acr_suffix.hex}"
  resource_group_name = azurerm_resource_group.main.name
  location             = azurerm_resource_group.main.location
  tags                 = var.tags
}

module "aks" {
  source = "../../modules/aks"

  cluster_name        = "aks-northwind-quote-dev"
  resource_group_name = azurerm_resource_group.main.name
  location             = azurerm_resource_group.main.location
  dns_prefix           = "northwind-quote-dev"
  subnet_id            = module.network_aks.subnet_id
  tags                 = var.tags
}

# Grants the AKS node pool's kubelet identity AcrPull on this project's
# registry -- the Terraform-native equivalent of `az aks update --attach-acr`,
# scoped to exactly this one registry rather than the broader role that
# command can imply.
resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = module.registry.id
  role_definition_name = "AcrPull"
  principal_id          = module.aks.kubelet_identity_object_id
}
