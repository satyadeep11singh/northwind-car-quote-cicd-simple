# modules/aks/outputs.tf

output "id" {
  value = azurerm_kubernetes_cluster.this.id
}

output "name" {
  value = azurerm_kubernetes_cluster.this.name
}

# The kubelet identity, not the cluster's own SystemAssigned identity, is
# what pulls images on the nodes' behalf -- this is the principal that needs
# AcrPull on the registry.
output "kubelet_identity_object_id" {
  value = azurerm_kubernetes_cluster.this.kubelet_identity[0].object_id
}

output "kube_config_raw" {
  value     = azurerm_kubernetes_cluster.this.kube_config_raw
  sensitive = true
}
