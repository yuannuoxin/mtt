package com.mtd.mjq.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.mtd.mjq.entity.SysUser;
import com.mtd.mjq.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true, rollbackFor = Throwable.class)
public class SysUserService {

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    //    @Transactional
    public SysUser create(SysUser user) {
        return userRepository.save(user);
    }

    public SysUser getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<SysUser> list() {
        return userRepository.findAll();
    }

    //    @Transactional
    public SysUser update(SysUser user) {
        return userRepository.save(user);
    }

    //    @Transactional
    public SysUser update2(SysUser user) {
        SysUser sysUser = userRepository.findById(user.getId()).get();
        BeanUtil.copyProperties(user, sysUser, CopyOptions.create());
        return userRepository.save(sysUser);
    }

    @Transactional(readOnly = true, rollbackFor = Throwable.class)
    public SysUser updateTest() {
        SysUser sysUser = userRepository.findById(1L).get();
        sysUser.setNickname(UUID.randomUUID().toString());

        TransactionTemplate newTx = new TransactionTemplate(platformTransactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // 👇 lambda 事务
        newTx.executeWithoutResult(status -> {
            userRepository.save(sysUser);
        });

        return sysUser;
    }

    //    @Transactional
    public void deleteById(Long id) {
        Optional<SysUser> SysUserOptional = userRepository.findById(id);
        if (SysUserOptional.isPresent()) {
            SysUser sysUser = SysUserOptional.get();
            sysUser.setDeleted(1);
            userRepository.save(sysUser);
        }
    }
}
