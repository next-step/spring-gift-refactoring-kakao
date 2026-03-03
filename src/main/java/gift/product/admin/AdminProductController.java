package gift.product.admin;

import gift.product.admin.ProductDto.CategoryDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    private final AdminProductNameValidator productNameValidator;

    @GetMapping
    public String list(Model model) {
        List<ProductDto> products = adminProductService.getAllProducts();

        model.addAttribute("products", products);

        return "product/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        List<CategoryDto> categories = adminProductService.getAllCategories();

        model.addAttribute("categories", categories);

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
        List<String> errors = productNameValidator.validate(name);

        if (!errors.isEmpty()) {
            List<CategoryDto> categories = adminProductService.getAllCategories();

            populateNewForm(model, errors, name, price, imageUrl, categoryId, categories);

            return "product/new";
        }

        adminProductService.createProduct(
                name, price, imageUrl, categoryId
        );

        return "redirect:/admin/products";
    }

    private void populateNewForm(
            Model model,
            List<String> errors,
            String name,
            int price,
            String imageUrl,
            Long categoryId,
            List<CategoryDto> categories
    ) {
        model.addAttribute("errors", errors);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categories);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductDto product = adminProductService.getProduct(id);
        List<CategoryDto> categories = adminProductService.getAllCategories();

        model.addAttribute("product", product);
        model.addAttribute("categories", categories);

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
        ProductDto productDto = adminProductService.getProduct(id);
        List<String> errors = productNameValidator.validate(name);

        if (!errors.isEmpty()) {
            List<CategoryDto> categories = adminProductService.getAllCategories();

            populateEditForm(
                    model, productDto, errors, name,
                    price, imageUrl, categoryId, categories
            );

            return "product/edit";
        }

        adminProductService.updateProduct(id, name, price, imageUrl, categoryId);

        return "redirect:/admin/products";
    }

    private void populateEditForm(
            Model model,
            ProductDto productDto,
            List<String> errors,
            String name,
            int price,
            String imageUrl,
            Long categoryId,
            List<CategoryDto> categories
    ) {
        model.addAttribute("errors", errors);
        model.addAttribute("product", productDto);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categories);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {

        adminProductService.deleteProduct(id);

        return "redirect:/admin/products";
    }
}
