package com.teammatch.m4.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teammatch.m4.entity.ProjectSkill;
import com.teammatch.m4.mapper.M4ProjectSkillMapper;
import com.teammatch.m4.service.ProjectSkillService;
import org.springframework.stereotype.Service;

@Service
public class ProjectSkillServiceImpl extends ServiceImpl<M4ProjectSkillMapper, ProjectSkill> implements ProjectSkillService {
}
