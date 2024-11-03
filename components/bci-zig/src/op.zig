pub const Op = enum(u8) {
    push_bool_true,
    push_bool_false,
    push_i32_literal,
    push_i32_stack,
    discard,

    jmp_dup_false,
    jmp_dup_true,

    not_bool,

    add_i32,
    sub_i32,
    mul_i32,
    div_i32,
    mod_i32,
    pow_i32,

    eq_bool,
    eq_i32,
    neq_bool,
    neq_i32,
    lt_i32,
    le_i32,
    gt_i32,
    ge_i32,

    println,
    print_bool,
    print_i32,
};
