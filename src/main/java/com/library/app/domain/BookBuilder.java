package com.library.app.domain;

public class BookBuilder {
    private String title;
    private String author;
    private String isbn;
    private Integer totalQuantity = 1;
    private Category category; 
    
    private String publisher;
    private Integer publishYear;
    private String image;

    public BookBuilder(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public BookBuilder setIsbn(String isbn) { this.isbn = isbn; return this; }
    public BookBuilder setTotalQuantity(Integer quantity) { this.totalQuantity = quantity; return this; }
    public BookBuilder setCategory(Category category) { 
        this.category = category; 
        return this; 
    }
    
    public BookBuilder setPublisher(String publisher) { this.publisher = publisher; return this; }
    public BookBuilder setPublishYear(Integer year) { this.publishYear = year; return this; }
    public BookBuilder setImage(String image) { this.image = image; return this; }

    public Book build() {
        Book book = new Book();
        book.setTitle(this.title);
        book.setAuthor(this.author);
        book.setIsbn(this.isbn);
        book.setTotalQuantity(this.totalQuantity);
        book.setAvailableQuantity(this.totalQuantity);
        book.setCategory(this.category);
        book.setPublisher(this.publisher);
        book.setPublishYear(this.publishYear);
        book.setImage(this.image);
        return book;
    }
}