rootProject.name = "repertorio"

include(
    "infrastructure:api-gateway",
    "infrastructure:discovery-server",
    "services:hermandad-service",
    "services:repertorio-service",
    "services:procesion-service",
    "services:tracking-service",
    "services:notification-service",
    "shared:common"
)
