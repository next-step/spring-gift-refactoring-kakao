package gift.product;

import gift.category.CategoryService;
import gift.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
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
        try {
            productService.create(name, price, imageUrl, categoryId, true);
            return "redirect:/admin/products";
        } catch (ApplicationException e) {
            populateNewForm(model, List.of(e.getMessage()), name, price, imageUrl, categoryId);
            return "product/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
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
        try {
            productService.update(id, name, price, imageUrl, categoryId, true);
            return "redirect:/admin/products";
        } catch (ApplicationException e) {
            Product product = productService.findById(id);
            populateEditForm(model, product, List.of(e.getMessage()), name, price, imageUrl, categoryId);
            return "product/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/products";
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
            Product product,
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
