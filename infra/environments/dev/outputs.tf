# environments/dev/outputs.tf

output "resource_group_name" {
  value = azurerm_resource_group.main.name
}

output "acr_name" {
  value = module.registry.name
}

output "acr_login_server" {
  value = module.registry.login_server
}

output "aks_cluster_name" {
  value = module.aks.name
}

output "kube_config_raw" {
  value     = module.aks.kube_config_raw
  sensitive = true
}
