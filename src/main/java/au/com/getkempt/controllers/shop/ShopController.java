package au.com.getkempt.controllers.shop;

import au.com.getkempt.request.shop.ShopRequest;
import au.com.getkempt.responses.ShopResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy placeholder. The real store/shop API lives in
 * {@link au.com.getkempt.controllers.store.StoreController} ({@code /api/v1/store/**}).
 * Kept as a stub only; a request body is only legal on a mutating verb, so this is a POST.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/shop")
@Slf4j
public class ShopController {

    @PostMapping
    public ShopResponse createShop(@RequestBody @Valid final ShopRequest shopRequest) {
        return new ShopResponse(shopRequest.id());
    }

}
