-- MS-Alertas V1: tabla alertas (spec 004 data-model.md)
CREATE TABLE alertas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  ciudad VARCHAR(100),
  tipo VARCHAR(20),
  operacion VARCHAR(20),
  precio_min DECIMAL(12,2),
  precio_max DECIMAL(12,2),
  estado VARCHAR(20) NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_alertas_usuario ON alertas (usuario_id);
CREATE INDEX idx_alertas_estado ON alertas (estado);
