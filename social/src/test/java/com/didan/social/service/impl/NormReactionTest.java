package com.didan.social.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** normReaction là static package-private trong PostServiceImpl. */
class NormReactionTest {

    @Test
    void nullBecomesLike() {
        assertEquals("LIKE", PostServiceImpl.normReaction(null));
    }

    @Test
    void unknownBecomesLike() {
        assertEquals("LIKE", PostServiceImpl.normReaction("thumbsup"));
        assertEquals("LIKE", PostServiceImpl.normReaction(""));
    }

    @Test
    void normalisesCaseAndWhitespace() {
        assertEquals("LOVE", PostServiceImpl.normReaction("love"));
        assertEquals("HAHA", PostServiceImpl.normReaction("  haha "));
        assertEquals("WOW", PostServiceImpl.normReaction("Wow"));
        assertEquals("SAD", PostServiceImpl.normReaction("SAD"));
        assertEquals("ANGRY", PostServiceImpl.normReaction("angry"));
        assertEquals("LIKE", PostServiceImpl.normReaction("like"));
    }
}
