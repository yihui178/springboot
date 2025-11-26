package com.example.mybatis.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.HighlightDTO;
import com.example.mybatis.entity.Highlight;
import java.util.List;
/**
 * 亮点服务接口
 * @author yihui
 */
public interface HighlightService extends IService<Highlight> {
    /**
     * 查询所有亮点（返回 DTO）
     */
    List<HighlightDTO> listAllHighlights();
    /**
     * 新增亮点
     */
    HttpResult<String> addHighlight(HighlightDTO dto);
    /**
     * 更新亮点
     */
    HttpResult<String> updateHighlight(HighlightDTO dto);
    /**
     * 删除亮点
     */
    HttpResult<String> deleteHighlight(Long highlightId);
}