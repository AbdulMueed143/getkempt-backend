package au.com.getkempt.controllers.shop;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping('/api/v1/shop')
public class ShopController {

    @GetMapping
    public ShopResponse createShop(@RequestBody  final ShopRequest shopRequest) {

    }

}
