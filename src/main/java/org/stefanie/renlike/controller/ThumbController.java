package org.stefanie.renlike.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stefanie.renlike.common.BaseResponse;
import org.stefanie.renlike.common.ResultUtils;
import org.stefanie.renlike.model.dto.DoThumbRequest;
import org.stefanie.renlike.service.ThumbService;

@RestController
@RequestMapping("thumb")
public class ThumbController {  
    @Resource
    private ThumbService thumbService;
  
    @PostMapping("/do")
    public BaseResponse<Boolean> doThumb(@RequestBody DoThumbRequest doThumbRequest, HttpServletRequest request) {
        Boolean success = thumbService.doThumb(doThumbRequest, request);  
        return ResultUtils.success(success);
    }
    @PostMapping("/undo")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoThumbRequest doThumbRequest, HttpServletRequest request) {
        Boolean success = thumbService.undoThumb(doThumbRequest, request);
        return ResultUtils.success(success);
    }

}
