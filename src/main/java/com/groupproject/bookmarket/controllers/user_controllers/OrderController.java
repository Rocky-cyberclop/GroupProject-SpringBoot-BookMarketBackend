package com.groupproject.bookmarket.controllers.user_controllers;

import com.groupproject.bookmarket.dtos.OrderHistoryDto;
import com.groupproject.bookmarket.dtos.TotalPricedto;
import com.groupproject.bookmarket.dtos.VnpPaymentDTO;
import com.groupproject.bookmarket.models.CartItem;
import com.groupproject.bookmarket.models.Order;
import com.groupproject.bookmarket.models.User;
import com.groupproject.bookmarket.repositories.UserRepository;
import com.groupproject.bookmarket.requests.CartRequest;
import com.groupproject.bookmarket.requests.OrderRequest;
import com.groupproject.bookmarket.responses.MessageResponse;
import com.groupproject.bookmarket.services.ConfigPaymenntService;
import com.groupproject.bookmarket.services.JwtService;
import com.groupproject.bookmarket.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/user/")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfigPaymenntService configPaymenntService;
    @GetMapping("cart/info")
    public ResponseEntity<?> getInfoCartUser(@RequestHeader(name = "Authorization") String token){
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()){
            throw new RuntimeException("User not found");
        }
        return orderService.getInfoCart(userOptional.get().getId());
    }

    @PostMapping("cart/add")
    public String addToCart(@RequestBody CartRequest cartRequest,@RequestHeader(name = "Authorization") String token){
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);

        }
        String email = jwtService.extractUsername(token);
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()){
            throw new RuntimeException("User not found");
        }
//        System.out.println(cartRequest.getQuantity());
        return orderService.addToCart(cartRequest,userOptional.get().getId());
    }


    @PostMapping("/cart/checkout")
    public ResponseEntity<String> checkOut(@RequestBody OrderRequest orderRequest,@RequestHeader(name = "Authorization") String token){

//        System.out.println(orderRequest.getCartItemIds());
//        System.out.println(orderRequest.getVoucherId());
//        System.out.println(orderRequest.getAddress());
//        System.out.println(orderRequest.getCode());
        System.out.println("Before extract token!");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        Optional<User> userOptional = userRepository.findByEmail(email);
        System.out.println("After extract token!");
        if (userOptional.isEmpty()){
            throw new RuntimeException("User not found");
        }
                System.out.println("User id" + userOptional.get().getId());
        return orderService.sendReceipt(orderRequest,userOptional.get().getId());
    }

    @GetMapping("order/history")
    public List<OrderHistoryDto> getOrdersByUser(@RequestHeader(name = "Authorization") String token) {
        return orderService.getOrdersByUser(token);
    }
    @GetMapping("/get/fullName")
    public Map<String, String> getFullNameAndAvatarUser(@RequestHeader(name = "Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOptional.get();

        Map<String, String> response = new HashMap<>();
        response.put("fullName", user.getFullName());
        response.put("avatarUrl", user.getAvatar()); // Assuming getAvatarUrl() is the method to get the avatar URL

        return response;
    }
    @PostMapping("/checkout/createUrl")
    public ResponseEntity<MessageResponse> createUrlPayment(@RequestBody TotalPricedto totalPricedto, @RequestHeader(name = "Authorization") String token) throws UnsupportedEncodingException {
        System.out.println(totalPricedto.getTotalPrice());
        System.out.println(token);
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()){
            throw new RuntimeException("User not found");
        }
        return configPaymenntService.createUrlPayment(userOptional.get().getEmail(),totalPricedto.getTotalPrice());
    }
    @PostMapping("/checkout/checkResponse")
    public ResponseEntity<?> createUrlPayment(@RequestBody VnpPaymentDTO requestData, @RequestHeader(name = "Authorization") String token ) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()){
            throw new RuntimeException("User not found");
        }
        return configPaymenntService.handlePaymentResult(requestData,userOptional.get().getEmail());
    }

    @GetMapping("voucher/{code}")
    public  ResponseEntity<?> getDiscountPercentAndIdByCode(@PathVariable String code){
        return orderService.getDiscountPercentAndIdByCode(code);
    }

    @PutMapping("/{cartItemId}/increment")
    public ResponseEntity<CartItem> incrementQuantity(@PathVariable Long cartItemId) {
        CartItem updatedCartItem = orderService.incrementQuantity(cartItemId);
        return new ResponseEntity<>(updatedCartItem, HttpStatus.OK);
    }
    // giảm số lượng
    @PutMapping("/{cartItemId}/decrement")
    public ResponseEntity<CartItem> decrementQuantity(@PathVariable Long cartItemId) {
        CartItem updatedCartItem = orderService.decrementQuantity(cartItemId);
        return new ResponseEntity<>(updatedCartItem, HttpStatus.OK);
    }

    //  xóa một mục khỏi giỏ hàng
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId) {
        orderService.deleteCartItem(cartItemId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
