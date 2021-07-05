package com.alibaba.chaosblade.box.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.chaosblade.box.collector.model.Query;
import com.alibaba.chaosblade.box.common.exception.BizException;
import com.alibaba.chaosblade.box.common.utils.SystemPropertiesUtils;
import com.alibaba.chaosblade.box.dao.model.ClusterDO;
import com.alibaba.chaosblade.box.dao.page.PageUtils;
import com.alibaba.chaosblade.box.dao.repository.ClusterRepository;
import com.alibaba.chaosblade.box.service.ClusterService;
import com.alibaba.chaosblade.box.service.collect.CollectorTimer;
import com.alibaba.chaosblade.box.service.model.cluster.ClusterBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.alibaba.chaosblade.box.common.exception.ExceptionMessageEnum.CLUSTER_CONNECT_ERROR;

/**
 * @author yefei
 */
@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService, InitializingBean {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private CollectorTimer collectorTimer;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public void afterPropertiesSet() {
        List<ClusterDO> clusters = clusterRepository.selectList(ClusterDO.builder()
                .isCollector(true)
                .build());
        clusters.forEach(cluster -> collectorTimer.collect(Query.builder()
                .config(cluster.getConfig())
                .clusterId(cluster.getId())
                .build()));
    }

    @Override
    public void addCluster(ClusterBO clusterBO) {

        try {
            collectorTimer.dryRun(clusterBO.getConfig());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BizException(CLUSTER_CONNECT_ERROR);
        }

        ClusterDO clusterDO = ClusterDO.builder()
                .clusterName(clusterBO.getClusterName())
                .config(clusterBO.getConfig())
                .isCollector(false)
                .build();
        clusterRepository.insert(clusterDO);

        String home = SystemPropertiesUtils.getPropertiesValue("user.home");

        FileUtil.writeString(clusterBO.getConfig(),
                String.format("%s/%s/%s/config", home, applicationName, clusterDO.getId()),
                SystemPropertiesUtils.getPropertiesFileEncoding());
    }

    @Override
    public List<ClusterBO> getClusterPageable(ClusterBO clusterBO) {
        PageUtils.startPage(clusterBO);

        ClusterDO clusterDO = ClusterDO.builder()
                .isCollector(clusterBO.getIsCollector())
                .clusterName(clusterBO.getClusterName())
                .build();
        return clusterRepository.selectList(clusterDO).stream().map(cluster -> {
            ClusterBO bo = ClusterBO.builder().build();
            BeanUtil.copyProperties(cluster, bo);
            return bo;
        }).collect(Collectors.toList());
    }

    @Override
    public void activeCollect(ClusterBO clusterBO) {
        Optional<ClusterDO> clusterDO = clusterRepository.selectById(clusterBO.getId());
        ClusterDO cluster = clusterDO.get();

        clusterRepository.updateByPrimaryKey(clusterBO.getId(), ClusterDO.builder()
                .isCollector(true)
                .build());

        collectorTimer.collect(Query.builder()
                .config(cluster.getConfig())
                .clusterId(cluster.getId())
                .build());
    }

    @Override
    public void closeCollect(ClusterBO clusterBO) {

        clusterRepository.updateByPrimaryKey(clusterBO.getId(), ClusterDO.builder()
                .isCollector(false)
                .build());

        Optional<ClusterDO> clusterDO = clusterRepository.selectById(clusterBO.getId());
        ClusterDO cluster = clusterDO.get();

        collectorTimer.stop(Query.builder()
                .config(cluster.getConfig())
                .clusterId(cluster.getId())
                .build());
    }

    @Override
    public void updateCluster(ClusterBO clusterBO) {

        try {
            collectorTimer.dryRun(clusterBO.getConfig());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BizException(CLUSTER_CONNECT_ERROR);
        }

        ClusterDO clusterDO = ClusterDO.builder()
                .clusterName(clusterBO.getClusterName())
                .config(clusterBO.getConfig())
                .build();
        clusterRepository.updateByPrimaryKey(clusterBO.getId(), clusterDO);

        String home = SystemPropertiesUtils.getPropertiesValue("user.home");
        FileUtil.writeString(clusterBO.getConfig(),
                String.format("%s/%s/%s/config", home, applicationName, clusterDO.getId()),
                SystemPropertiesUtils.getPropertiesFileEncoding());
    }

    @Override
    public String getKubeconfig(ClusterBO clusterBO) {
        String home = SystemPropertiesUtils.getPropertiesValue("user.home");
        String kubeconfig = String.format("%s%s%s%s%s%sconfig", home, File.separatorChar, applicationName, File.separatorChar, clusterBO.getId(), File.separatorChar);
        if (!FileUtil.exist(kubeconfig)) {
            Optional<ClusterDO> clusterDO = clusterRepository.selectById(clusterBO.getId());
            ClusterDO cluster = clusterDO.get();

            FileUtil.writeString(cluster.getConfig(),
                    String.format("%s%s%s%s%s%sconfig", home, File.separatorChar, applicationName, File.separatorChar, cluster.getId(), File.separatorChar),
                    SystemPropertiesUtils.getPropertiesFileEncoding());
        }
        return kubeconfig;
    }
}
