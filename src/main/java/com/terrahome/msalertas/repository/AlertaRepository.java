package com.terrahome.msalertas.repository;

import com.terrahome.msalertas.model.entity.Alerta;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    Page<Alerta> findByUsuarioId(Long usuarioId, Pageable pageable);
    List<Alerta> findByUsuarioId(Long usuarioId);
}
