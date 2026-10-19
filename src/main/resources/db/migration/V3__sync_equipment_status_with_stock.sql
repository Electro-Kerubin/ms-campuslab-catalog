-- Un equipo sin unidades disponibles figura como EN_USO (desde ahora lo mantiene el servicio al mover stock).
-- Alinea los equipos que ya estaban agotados.
UPDATE resources r
SET status = 'EN_USO', updated_at = now()
FROM resource_stock s
WHERE s.resource_id = r.id
  AND r.resource_type = 'EQUIPO'
  AND r.status = 'DISPONIBLE'
  AND s.quantity_available = 0;
