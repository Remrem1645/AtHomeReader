package com.rem.reader.Service;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import com.rem.reader.Repo.BookPageCacheRepo;

@Service
public class BookPageCacheService {

    @Autowired
    BookPageCacheRepo bookPageCacheRepo;
}
