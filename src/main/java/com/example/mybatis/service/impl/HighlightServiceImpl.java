package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.HighlightMapper;
import com.example.mybatis.service.HighlightService;
import org.springframework.stereotype.Service;

@Service
public class HighlightServiceImpl extends ServiceImpl<HighlightMapper, Highlight> implements HighlightService {}
