# modules/registry/variables.tf

variable "registry_name" {
  description = "Globally-unique ACR name (alphanumeric only, 5-50 chars)."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "tags" {
  type    = map(string)
  default = {}
}
