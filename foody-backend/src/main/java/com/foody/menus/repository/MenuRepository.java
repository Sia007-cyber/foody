package com.foody.menus.repository;

import com.foody.menus.entity.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByBusinessIdOrderByDisplayOrderAsc(Long businessId);
}
