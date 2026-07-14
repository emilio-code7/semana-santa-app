package com.repertorio.marcha.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MarchaTest {

    @Test
    void createWithValidFields() {
        var marcha = Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO,
                420, 1919, "https://youtube.com/amarguras");
        assertNotNull(marcha.getId());
        assertEquals("Amarguras", marcha.getTitle());
        assertEquals("Manuel López Farfán", marcha.getComposer());
        assertEquals(BandType.BANDA_PALIO, marcha.getBandType());
        assertEquals(420, marcha.getDurationSeconds());
        assertEquals(1919, marcha.getCompositionYear());
        assertEquals("https://youtube.com/amarguras", marcha.getYoutubeUrl());
        assertNotNull(marcha.getCreatedAt());
        assertNotNull(marcha.getUpdatedAt());
    }

    @Test
    void createWithBlankTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                Marcha.create("", "Manuel López Farfán", BandType.BANDA_PALIO, 420, 1919, null));
    }

    @Test
    void createWithNullTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                Marcha.create(null, "Manuel López Farfán", BandType.BANDA_PALIO, 420, 1919, null));
    }

    @Test
    void createWithBlankComposer() {
        assertThrows(IllegalArgumentException.class, () ->
                Marcha.create("Amarguras", "", BandType.BANDA_PALIO, 420, 1919, null));
    }

    @Test
    void createWithNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO, -1, 1919, null));
    }

    @Test
    void createWithZeroDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO, 0, 1919, null));
    }

    @Test
    void createWithNullBandType() {
        assertThrows(IllegalArgumentException.class, () ->
                Marcha.create("Amarguras", "Manuel López Farfán", null, 420, 1919, null));
    }

    @Test
    void createWithNullCompositionYear() {
        var marcha = Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO,
                420, null, null);
        assertNull(marcha.getCompositionYear());
    }

    @Test
    void updateChangesFields() {
        var marcha = Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO,
                420, 1919, null);
        marcha.update("Saeta", "Joaquín Turina", BandType.BANDA_CORNETAS, 300, 1930, "https://youtube.com/saeta");

        assertEquals("Saeta", marcha.getTitle());
        assertEquals("Joaquín Turina", marcha.getComposer());
        assertEquals(BandType.BANDA_CORNETAS, marcha.getBandType());
        assertEquals(300, marcha.getDurationSeconds());
        assertEquals(1930, marcha.getCompositionYear());
    }

    @Test
    void updateWithBlankTitle() {
        var marcha = Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO,
                420, 1919, null);
        assertThrows(IllegalArgumentException.class, () ->
                marcha.update("", "Joaquín Turina", BandType.BANDA_CORNETAS, 300, 1930, null));
    }

    @Test
    void updateTicksUpdatedAt() throws InterruptedException {
        var marcha = Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO,
                420, 1919, null);
        var beforeUpdate = marcha.getUpdatedAt();
        Thread.sleep(1);
        marcha.update("Saeta", "Joaquín Turina", BandType.BANDA_CORNETAS, 300, 1930, null);
        assertTrue(marcha.getUpdatedAt().isAfter(beforeUpdate));
    }
}
