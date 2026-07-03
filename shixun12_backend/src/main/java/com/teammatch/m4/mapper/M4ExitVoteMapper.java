package com.teammatch.m4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.m4.entity.ExitVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface M4ExitVoteMapper extends BaseMapper<ExitVote> {

    /** T-141: 行级锁，防止 closeVote 并发双重执行 */
    @Select("SELECT * FROM exit_vote WHERE id = #{id} FOR UPDATE")
    ExitVote selectByIdForUpdate(Long id);
}
