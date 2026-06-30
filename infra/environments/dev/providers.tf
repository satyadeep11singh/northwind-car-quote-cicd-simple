# environments/dev/providers.tf
#
# Reuses the persistent state backend shared across this portfolio's
# projects (see PROJECT4-CLAUDE-CODE-HANDOFF.md), with a key distinct from
# every other project's state in the same container.

terraform {
  required_version = ">= 1.7.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.95"
    }
  }

  backend "azurerm" {
    resource_group_name  = "rg-northwind-tfstate"
    storage_account_name = "stnorthwindtf676746"
    container_name       = "tfstate"
    key                  = "quote-generator-dev.tfstate"
  }
}

provider "azurerm" {
  features {}
}
