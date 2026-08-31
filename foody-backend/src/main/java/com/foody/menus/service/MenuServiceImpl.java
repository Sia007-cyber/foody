package com.foody.menus.service;

import com.foody.menus.entity.Menu;
import com.foody.menus.repository.MenuRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;

    MenuServiceImpl(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> findByBusinessId(Long businessId) {
        return menuRepository.findByBusinessIdOrderByDisplayOrderAsc(businessId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Menu> findById(Long id) {
        return menuRepository.findById(id);
    }
}
