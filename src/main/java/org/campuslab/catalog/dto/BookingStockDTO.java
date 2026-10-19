package org.campuslab.catalog.dto;

/**
 * Resultado de tomar o liberar el stock de un recurso para una reserva.
 *
 * @param stockManaged       false si el recurso no descuenta stock por reserva (sala, insumo)
 * @param changed            false si la operación no hizo nada (recurso sin stock, o ya estaba aplicada)
 * @param quantityAvailable  unidades disponibles después de la operación (null si no maneja stock)
 */
public record BookingStockDTO(
        Long resourceId,
        Long bookingId,
        boolean stockManaged,
        boolean changed,
        Integer quantityAvailable
) {
    public static BookingStockDTO unmanaged(Long resourceId, Long bookingId) {
        return new BookingStockDTO(resourceId, bookingId, false, false, null);
    }
}
