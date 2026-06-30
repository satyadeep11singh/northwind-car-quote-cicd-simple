# modules/aks/variables.tf

variable "cluster_name" {
  type = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "dns_prefix" {
  type = string
}

variable "subnet_id" {
  description = "Subnet the node pool's NICs are attached to."
  type        = string
}

variable "node_count" {
  type    = number
  default = 1
}

variable "node_vm_size" {
  type    = string
  default = "Standard_B2pls_v2"
}

variable "tags" {
  type    = map(string)
  default = {}
}
