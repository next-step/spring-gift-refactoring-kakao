package gift.product;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gift.category.CategoryService;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAllProducts());
        return "product/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "product/new";
    }

    @PostMapping
    public String create(
        @RequestParam String name,
        @RequestParam int price,
        @RequestParam String imageUrl,
        @RequestParam Long categoryId,
        Model model
    ) {
        List<String> errors = ProductNameValidator.validate(name, NamePolicy.ALLOW_KAKAO);
        if (!errors.isEmpty()) {
            populateNewForm(model, errors, name, price, imageUrl, categoryId);
            return "product/new";
        }

        createOrUpdateProduct(() -> productService.create(new ProductRequest(name, price, imageUrl, categoryId), NamePolicy.ALLOW_KAKAO), categoryId);
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductResponse product = findProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAll());
        return "product/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable Long id,
        @RequestParam String name,
        @RequestParam int price,
        @RequestParam String imageUrl,
        @RequestParam Long categoryId,
        Model model
    ) {
        List<String> errors = ProductNameValidator.validate(name, NamePolicy.ALLOW_KAKAO);
        if (!errors.isEmpty()) {
            ProductResponse product = findProductById(id);
            populateEditForm(model, product, errors, name, price, imageUrl, categoryId);
            return "product/edit";
        }

        createOrUpdateProduct(() -> productService.update(id, new ProductRequest(name, price, imageUrl, categoryId), NamePolicy.ALLOW_KAKAO), categoryId);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/products";
    }

    private void createOrUpdateProduct(Runnable action, Long categoryId) {
        try {
            action.run();
        } catch (NoSuchElementException e) {
            if (e.getMessage().contains("카테고리")) {
                throw new NoSuchElementException(e.getMessage() + " id=" + categoryId);
            }
            throw e;
        }
    }

    private ProductResponse findProductById(Long id) {
        try {
            return productService.findById(id);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException(e.getMessage() + " id=" + id);
        }
    }

    private void populateNewForm(
        Model model,
        List<String> errors,
        String name,
        int price,
        String imageUrl,
        Long categoryId
    ) {
        model.addAttribute("errors", errors);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categoryService.findAll());
    }

    private void populateEditForm(
        Model model,
        ProductResponse product,
        List<String> errors,
        String name,
        int price,
        String imageUrl,
        Long categoryId
    ) {
        model.addAttribute("errors", errors);
        model.addAttribute("product", product);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categoryService.findAll());
    }
}
