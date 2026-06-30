# modules/aks/main.tf
#
# Single ARM64 node, SystemAssigned cluster identity (which provisions its
# own separate kubelet identity -- that kubelet identity, not the cluster
# identity, is what needs AcrPull on the registry; see outputs.tf and the
# role assignment in environments/dev/main.tf).
#
# Node count is fixed at 1 rather than this subscription's planned 2-node
# design: the canadacentral region's Bpsv2-family quota on this subscription
# is 4 vCPUs total, and Standard_B2pls_v2 is 2 vCPUs each -- a 2-node pool
# would consume the entire regional quota with zero headroom for anything
# else. Documented as a deferred hardening step in the README, not a
# silent simplification.

resource "azurerm_kubernetes_cluster" "this" {
  name                = var.cluster_name
  resource_group_name = var.resource_group_name
  location            = var.location
  dns_prefix          = var.dns_prefix
  sku_tier            = "Free"
  tags                = var.tags

  default_node_pool {
    name           = "system"
    node_count     = var.node_count
    vm_size        = var.node_vm_size
    vnet_subnet_id = var.subnet_id
  }

  identity {
    type = "SystemAssigned"
  }
}
