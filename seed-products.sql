-- ==============================================================
-- SCRIPT DE SEMILLA DE DATOS DE PRUEBA: PEDIDOS360 COFFEE STORE
-- ==============================================================
-- Base de datos destino: pedidos360_catalog
-- Tabla: products (name, description, price, stock, image_url)
-- ==============================================================

-- Asegurar creación de la tabla si no existe
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price NUMERIC(12, 2) NOT NULL,
    stock INTEGER NOT NULL,
    image_url VARCHAR(1000)
);

-- Limpiar datos previos si se desea reiniciar el catálogo (opcional)
-- TRUNCATE TABLE products RESTART IDENTITY CASCADE;

-- Insertar catálogo de productos de café y accesorios
INSERT INTO products (name, description, price, stock, image_url) VALUES
-- Cafés de Origen / Especialidad en Grano y Molido
('Café Espresso Roast Blend 250g', 
 'Café de tueste oscuro con notas intensas de chocolate negro, caramelo y cuerpo denso. Ideal para preparaciones en cafetera espresso y moka italiana.', 
 7490.00, 45, 'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=500'),

('Café Colombia Huila Especial 500g', 
 'Origen 100% Arábica de la región del Huila. Perfil equilibrado con acidez cítrica brillante, fragancia a jazmín y notas a panela dulce.', 
 11990.00, 30, 'https://images.unsplash.com/photo-1587734195503-904fca47e0e9?w=500'),

('Café Etiopía Yirgacheffe 250g', 
 'Variedad Heirloom de tueste medio-claro. Perfil floral y frutal excepcional con notas a bergamota, melocotón y té negro. Perfecto para filtrados V60 y Chemex.', 
 9290.00, 20, 'https://images.unsplash.com/photo-1610632380989-680fe40816c6?w=500'),

('Café Brasil Santos Mogiana 1kg', 
 'Formato económico de 1 kilo en grano entero. Café suave de baja acidez con perfil achocolatado, avellanas tostadas y gran crema. Excelente para consumo diario.', 
 18990.00, 50, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500'),

('Café Geisha Panamá Boquete Reserva 250g', 
 'Edición limitada de microlote de alta gama (89+ puntos SCA). Notas elegantes a flor de café, jazmín, lima kaffir y albaricoque.', 
 19990.00, 12, 'https://images.unsplash.com/photo-1518832553480-cd0e625ed3e6?w=500'),

('Café Descafeinado Swiss Water 250g', 
 'Café descafeinado 100% natural mediante proceso de agua sin químicos. Conserva cuerpo redondo y sabor a nuez, cacao y canela.', 
 8490.00, 25, 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=500'),

('Café Cold Brew Blend Grano Grueso 500g', 
 'Molienda gruesa diseñada especialmente para maceración en frío por 18 a 24 horas. Extrae una bebida refrescante, dulce y sin amargor.', 
 10490.00, 35, 'https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500'),

-- Accesorios y Métodos de Extracción
('Cafetera Prensa Francesa 600ml', 
 'Estructura de acero inoxidable y vidrio de borosilicato resistente al calor. Sistema de émbolo con triple filtro para una extracción con cuerpo completo.', 
 14990.00, 15, 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500'),

('Cafetera Italiana Moka 6 Tazas', 
 'Cafetera clásica de aluminio fundido apta para cocina a gas y eléctrica. Prepara un café concentrado, aromático y con carácter en pocos minutos.', 
 16490.00, 18, 'https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?w=500'),

('Molinillo Manual de Café con Muelas Cerámicas', 
 'Molinillo portátil con selector de molienda ajustable (fina para espresso hasta gruesa para prensa). Muelas cónicas de cerámica que evitan calentar el grano.', 
 17990.00, 22, 'https://images.unsplash.com/photo-1520970014086-2208d157c9e2?w=500'),

('Kit de Filtro V60 Cerámica + 40 Filtros de Papel', 
 'Gotero cónico de cerámica estriada tamaño 02 en color blanco mate. Incluye paquete de 40 filtros de papel de fibra natural sin blanquear.', 
 13990.00, 20, 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=500'),

('Taza Térmica Barista Doble Pared Inox 350ml', 
 'Taza aislada al vacío con tapa hermética antifugas. Mantiene el café caliente por más de 4 horas o frío por 8 horas.', 
 8990.00, 40, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500')
ON CONFLICT DO NOTHING;
