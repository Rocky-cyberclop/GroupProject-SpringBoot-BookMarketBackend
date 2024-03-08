package com.groupproject.bookmarket.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupproject.bookmarket.models.*;
import com.groupproject.bookmarket.repositories.*;
import com.groupproject.bookmarket.requests.BookRequest;
import com.groupproject.bookmarket.responses.*;
import com.groupproject.bookmarket.services.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class BookServiceImpl implements BookService {
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private FilesStorageServiceImpl filesStorageService;
    @Autowired
    private CommentRepository commentRepository;

    @Override
    public ResponseEntity<PaginationResponse> searchPaginateByTitle(String title, int size, int cPage) {
        if ( title == null || title.isEmpty()) {
            title = "%";
        } else {
            title = "%" + title + "%";
        }
        Pageable pageable = PageRequest.of(cPage - 1, size);
        Page<Book> page = bookRepository.findByTitleLikeAndIsDeleteFalse(pageable, title);
        Pagination pagination = Pagination.builder()
                .currentPage(cPage)
                .size(size)
                .totalPage(page.getTotalPages())
                .totalResult((int) page.getTotalElements())
                .build();
        PaginationResponse paginationResponse = PaginationResponse.builder()
                .data(page.getContent())
                .pagination(pagination)
                .build();
        return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<MyResponse> addNewBook(List<MultipartFile> images, String addBookRequest) throws JsonProcessingException {
        MyResponse myResponse = new MyResponse();
        ObjectMapper objectMapper = new ObjectMapper();
        BookRequest request = objectMapper.readValue(addBookRequest, BookRequest.class);

        Optional<Book> bookOptional = bookRepository.findByTitle(request.getTitle());
        if (bookOptional.isPresent()) {
            myResponse.setMessage("This book is already exist!");
            return new ResponseEntity<>(myResponse, HttpStatus.OK);
        }

        List<Author> authors = authorRepository.findAllById(request.getAuthorIds());
        List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
        LocalDate publishDate = LocalDate.parse(request.getPublishDate());

        Book newBook = new Book();
        newBook.setTitle(request.getTitle());
        newBook.setDescription(request.getDescription());
        newBook.setLanguage(request.getLanguage());
        newBook.setPublishDate(publishDate);
        newBook.setLastUpdate(LocalDate.now());
        newBook.setPrice(request.getPrice());
        newBook.setQuantity(request.getQuantity());
        newBook.setIsDelete(false);
        newBook.setAuthors(authors);
        newBook.setGenres(genres);

        Book bookSaved = bookRepository.save(newBook);

        List<Image> imageList = new ArrayList<>();
        images.forEach(image -> {
            Image newImage = new Image();
            newImage.setId(null);
            newImage.setUrl("http://localhost:8080/uploads/images/book/" + image.getOriginalFilename());
            newImage.setBook(bookSaved);
            imageList.add(newImage);
        });
        filesStorageService.saveBookImages(images);
        imageRepository.saveAll(imageList);

        myResponse.setMessage("Add new book successfully!");
        return new ResponseEntity<>(myResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Book> fetchBookInfo(Long bookId) {
        Optional<Book> book = bookRepository.findById(bookId);
        return book.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.OK));
    }

    @Override
    public ResponseEntity<MyResponse> editBook(Long bookId, List<MultipartFile> images, String addBookRequest) throws JsonProcessingException {
        MyResponse myResponse = new MyResponse();
        ObjectMapper objectMapper = new ObjectMapper();
        BookRequest request = objectMapper.readValue(addBookRequest, BookRequest.class);

        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isPresent()) {
            List<Author> authors = authorRepository.findAllById(request.getAuthorIds());
            List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
            LocalDate publishDate = LocalDate.parse(request.getPublishDate());

            bookOptional.get().setTitle(request.getTitle());
            bookOptional.get().setDescription(request.getDescription());
            bookOptional.get().setLanguage(request.getLanguage());
            bookOptional.get().setPublishDate(publishDate);
            bookOptional.get().setLastUpdate(LocalDate.now());
            bookOptional.get().setPrice(request.getPrice());
            bookOptional.get().setQuantity(request.getQuantity());
            bookOptional.get().setIsDelete(false);
            bookOptional.get().setAuthors(authors);
            bookOptional.get().setGenres(genres);

            //delete images
            if (images != null) {
                List<Image> listOldImages = bookOptional.get().getImages();
                List<String> listOldNameImages = listOldImages.stream().map(image -> {
                    if (image.getUrl().contains("http://localhost:8080/uploads/images/book/")) {
                        return image.getUrl().substring(42);
                    } else {
                        return null;
                    }
                }).toList();

                if (listOldNameImages.get(0) != null) {
                    listOldImages.clear();
                    bookOptional.get().setImages(listOldImages);
                    filesStorageService.deleteBookImages(listOldNameImages);
                }
                Book bookSaved = bookRepository.save(bookOptional.get());


                List<Image> imageList = new ArrayList<>();
                images.forEach(image -> {
                    Image newImage = new Image();
                    newImage.setId(null);
                    newImage.setUrl("http://localhost:8080/uploads/images/book/" + image.getOriginalFilename());
                    newImage.setBook(bookSaved);
                    imageList.add(newImage);
                });
                filesStorageService.saveBookImages(images);
                imageRepository.saveAll(imageList);
            }

            myResponse.setMessage("Edit book successfully!");
            return new ResponseEntity<>(myResponse, HttpStatus.OK);
        } else {
            myResponse.setMessage("This book is not exist!");
            return new ResponseEntity<>(myResponse, HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<List<Image>> fetchAllImagesByBookId(Long bookId) {
        Optional<Book> book = bookRepository.findById(bookId);
        return book.map(value -> new ResponseEntity<>(value.getImages(), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
    }

    @Override
    @Transactional
    public ResponseEntity<MyResponse> deleteBooks(List<Long> bookIds) {
        MyResponse myResponse = new MyResponse();
        List<Book> books = bookRepository.findAllById(bookIds);
        books.forEach(book -> book.setIsDelete(true));
        bookRepository.saveAll(books);
        myResponse.setMessage("Delete books successfully!!");
        return new ResponseEntity<>(myResponse, HttpStatus.OK);
    }

    @Override
    public List<ListBook> getListBook(int n) {
        Pageable pageable = PageRequest.of(0, n); // Page số 0, với 'limit' số lượng sách
        Page<Book> bookPage = bookRepository.findAll(pageable);
        List<Book> books = bookPage.getContent(); // Lấy nội dung từ Page<Book>
        return books.stream().map(book -> {
            ListBook listBook = new ListBook();
            listBook.setBookId(book.getId());
            listBook.setBookName(book.getTitle());
            listBook.setBookPrice(book.getPrice());
            List<Image> images = imageRepository.findByBookId(book.getId());
            listBook.setBookImage(images);
            return listBook;
        }).collect(Collectors.toList());
    }

    @Override
    public DetailBook getDetailBook(Long bookId){
        Book book = bookRepository.findById(bookId).orElse(null);
        return (book != null) ? convertDetailBook(book) : null;
    }

    private DetailBook convertDetailBook(Book book){
        DetailBook detailBook = new DetailBook();
        detailBook.setBookId(book.getId());
        detailBook.setBookTitle(book.getTitle());
        detailBook.setBookDescription(book.getDescription());
        detailBook.setBookQuantity(book.getQuantity());
        detailBook.setBookPrice(book.getPrice());
        List<Author> author = authorRepository.findAuthorByBooksId(book.getId());
        detailBook.setBookAuthorList(author);
        List<Genre> genresList = genreRepository.findByBooksId(book.getId());
        detailBook.setBookGenres(genresList);
        List<Image> imageList = imageRepository.findByBookId(book.getId());
        detailBook.setBookImage(imageList);
        List<Comment> commentList =commentRepository.findByBookId(book.getId());
        detailBook.setBookComment(commentList);
        return detailBook;
    }


}
