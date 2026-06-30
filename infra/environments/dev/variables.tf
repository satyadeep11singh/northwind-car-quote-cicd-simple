# environments/dev/variables.tf

variable "location" {
  type    = string
  default = "canadacentral"
}

variable "resource_group_name" {
  type    = string
  default = "rg-northwind-quote-dev"
}

variable "tags" {
  type = map(string)
  default = {
    Environment = "dev"
    Project     = "northwind-quote-generator"
    Owner       = "satyadeep"
  }
}
