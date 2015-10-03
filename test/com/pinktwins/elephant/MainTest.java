package com.pinktwins.elephant;

import org.junit.Test;

import com.pinktwins.elephant.data.Tag;

import static org.junit.Assert.*;

public class MainTest {
        @Test
        public void test1(){
                assertEquals("hello world!", "hello world!");
        }
        
        @Test
        public void testTagName(){
        	Tag tag = new Tag("tag");
        	assertEquals("tag",tag.name());
        }
        
        /*
         * failing test (build should show an error)
         * @Test
         * public void test2(){
         * assertEquals("this is","false");
         * }
         */
}


