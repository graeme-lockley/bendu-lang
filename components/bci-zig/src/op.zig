pub const Op = enum(u8) {
    push_i32_literal,
    push_i32_stack,
    add_i32,
    sub_i32,
    mul_i32,
    div_i32,
    mod_i32,
    pow_i32,
    print_i32,
    println,
};
