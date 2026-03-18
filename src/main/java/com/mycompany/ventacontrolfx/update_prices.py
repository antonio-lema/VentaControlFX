import os

def update_signatures(file_path, is_repo_impl=False):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Update Signatures
    content = content.replace("double multiplier, String reason)", "double multiplier, String reason, java.time.LocalDateTime startDate)")
    content = content.replace("double amount, String reason)", "double amount, String reason, java.time.LocalDateTime startDate)")
    content = content.replace("double roundingTarget, String reason)", "double roundingTarget, String reason, java.time.LocalDateTime startDate)")
    content = content.replace("double targetDecimal, String reason)", "double targetDecimal, String reason, java.time.LocalDateTime startDate)")
    content = content.replace("boolean isPercentage)", "boolean isPercentage, java.time.LocalDateTime startDate)")

    # 2. Fix JdbcPriceRepository internal uses
    if is_repo_impl:
        content = content.replace("LocalDateTime now = LocalDateTime.now();", "")
        content = content.replace(", now, ", ", startDate, ")
        content = content.replace("executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, now, globalTax)", "executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate, globalTax)")

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

# Update Interface
update_signatures("c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/src/main/java/com/mycompany/ventacontrolfx/domain/repository/IPriceRepository.java")

# Update Repository Impl
update_signatures("c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/src/main/java/com/mycompany/ventacontrolfx/infrastructure/persistence/JdbcPriceRepository.java", is_repo_impl=True)

# Update UseCase callsites (Signatures)
update_signatures("c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/src/main/java/com/mycompany/ventacontrolfx/application/usecase/MassivePriceUpdateUseCase.java")

print("Signature updates completed!")
