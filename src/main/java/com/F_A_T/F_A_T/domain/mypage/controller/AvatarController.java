package com.F_A_T.F_A_T.domain.mypage.controller;

import com.F_A_T.F_A_T.domain.mypage.storage.AvatarStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AvatarController {
    private final AvatarStorage storage;

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> read(@PathVariable String filename) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache()).body(storage.read(filename));
    }
}
