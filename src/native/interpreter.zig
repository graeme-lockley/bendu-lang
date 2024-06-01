const std = @import("std");

const AST = @import("../ast.zig");
const SP = @import("../string_pool.zig");

const LLVM = @cImport({
    @cInclude("llvm-c/Core.h");
    @cInclude("llvm-c/Target.h");
    @cInclude("llvm-c/ExecutionEngine.h");
});

pub fn eval(ast: *AST.Expression, allocator: std.mem.Allocator) !void {
    var state = CompilerState.init(
        "bendu",
        allocator,
    );
    defer state.deinit();

    // registerPrintInt(module);
    // _ = register(state, "_printInt", LLVM.LLVMVoidType(), [_]?*LLVM.struct_LLVMOpaqueType{LLVM.LLVMInt32Type()}, 1, false);
    _ = register(state, "_printLn", LLVM.LLVMVoidType(), null, 0, false);
    // var argsArray = [0]?*LLVM.struct_LLVMOpaqueType{};
    // _ = LLVM.LLVMAddFunction(state.module, "_printLn", LLVM.LLVMFunctionType(
    //     LLVM.LLVMVoidType(),
    //     &argsArray,
    //     0,
    //     0,
    // ));

    try createGlobals(ast, &state);
    try createMain(ast, &state);

    LLVM.LLVMDumpModule(state.module);
}

// fn registerPrintInt(module: LLVM.LLVMModuleRef) void {
//     var argsArray = [_]?*LLVM.struct_LLVMOpaqueType{ LLVM.LLVMInt32Type(), null };
//     const args: [*c]?*LLVM.struct_LLVMOpaqueType = @as([*c]?*LLVM.struct_LLVMOpaqueType, &argsArray);
//     _ = LLVM.LLVMAddFunction(module, "_printInt", LLVM.LLVMFunctionType(
//         LLVM.LLVMVoidType(),
//         args,
//         1,
//         0,
//     ));
// }

// fn registerPrintLn(module: LLVM.LLVMModuleRef) void {
//     var argsArray = [_]?*LLVM.struct_LLVMOpaqueType{null};

//     _ = LLVM.LLVMAddFunction(module, "_printLn", LLVM.LLVMFunctionType(
//         LLVM.LLVMVoidType(),
//         &(argsArray[0]),
//         0,
//         0,
//     ));
// }

fn register(state: CompilerState, name: [*c]const u8, returnType: LLVM.LLVMTypeRef, params: [*c]LLVM.LLVMTypeRef, paramCount: c_uint, isVarArg: bool) LLVM.LLVMValueRef {
    return LLVM.LLVMAddFunction(state.module, name, LLVM.LLVMFunctionType(
        returnType,
        params,
        paramCount,
        if (isVarArg) 1 else 0,
    ));
}

const CompilerState = struct {
    allocator: std.mem.Allocator,
    builder: LLVM.LLVMBuilderRef,
    module: LLVM.LLVMModuleRef,
    bindings: std.AutoHashMap(*SP.String, LLVM.LLVMValueRef),

    fn init(name: [*c]const u8, allocator: std.mem.Allocator) CompilerState {
        _ = LLVM.LLVMInitializeNativeTarget();
        _ = LLVM.LLVMInitializeNativeAsmPrinter();
        _ = LLVM.LLVMInitializeNativeAsmParser();
        LLVM.LLVMLinkInMCJIT();

        return CompilerState{
            .allocator = allocator,
            .module = LLVM.LLVMModuleCreateWithName(name),
            .builder = LLVM.LLVMCreateBuilder(),
            .bindings = std.AutoHashMap(*SP.String, LLVM.LLVMValueRef).init(allocator),
        };
    }

    fn deinit(self: *CompilerState) void {
        LLVM.LLVMDisposeBuilder(self.builder);
        LLVM.LLVMDisposeModule(self.module);

        self.bindings.deinit();
    }
};

fn createGlobals(ast: *AST.Expression, state: *CompilerState) !void {
    if (ast.kind == .block) {
        for (ast.kind.block.exprs) |expr| {
            if (expr.kind == .idDeclaration) {
                if (state.bindings.get(expr.kind.idDeclaration.name)) |_| {
                    try std.io.getStdErr().writer().print("Error: Attempt to redefine {s}\n", .{expr.kind.idDeclaration.name.slice()});
                    std.process.exit(1);
                } else {
                    const name = try std.mem.Allocator.dupeZ(state.allocator, u8, expr.kind.idDeclaration.name.slice());
                    defer state.allocator.free(name);

                    const binding = LLVM.LLVMAddGlobal(state.module, LLVM.LLVMInt32Type(), name);
                    LLVM.LLVMSetInitializer(binding, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0));

                    try state.bindings.put(expr.kind.idDeclaration.name, binding);

                    // LLVM.LLVMSetLinkage(global, LLVM.LLVMExternalLinkage);
                }
            }
        }
    }
}

fn createMain(ast: *AST.Expression, state: *const CompilerState) !void {
    const mainFnType = LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), null, 0, 0);
    const mainFn = LLVM.LLVMAddFunction(state.module, "_main", mainFnType);
    const entry = LLVM.LLVMAppendBasicBlock(mainFn, "entry");

    LLVM.LLVMPositionBuilderAtEnd(state.builder, entry);

    _ = LLVM.LLVMBuildRet(state.builder, try compileExpr(ast, state));
}

fn compileExpr(ast: *AST.Expression, state: *const CompilerState) !LLVM.LLVMValueRef {
    switch (ast.kind) {
        .block => {
            var result: ?LLVM.LLVMValueRef = null;
            for (ast.kind.block.exprs) |expr| {
                result = try compileExpr(expr, state);
            }
            return result orelse LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0);
        },
        .idDeclaration => {
            const name = ast.kind.idDeclaration.name;
            const binding = state.bindings.get(name) orelse {
                try std.io.getStdErr().writer().print("Error: Attempt to use undeclared variable {s}\n", .{name.slice()});
                std.process.exit(1);
            };
            return LLVM.LLVMBuildStore(state.builder, try compileExpr(ast.kind.idDeclaration.value, state), binding);
        },
        .identifier => {
            const name = ast.kind.identifier.name;
            const binding = state.bindings.get(name) orelse {
                try std.io.getStdErr().writer().print("Error: Attempt to use undeclared variable {s}\n", .{name.slice()});
                std.process.exit(1);
            };
            return LLVM.LLVMBuildLoad2(state.builder, LLVM.LLVMInt32Type(), binding, "");
        },
        .literalInt => return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), @intCast(ast.kind.literalInt.value), 0),
        .println => {
            const println = LLVM.LLVMGetNamedFunction(state.module, "_printLn") orelse {
                try std.io.getStdErr().writer().print("Error: _printLn not found\n", .{});
                std.process.exit(1);
            };

            try std.io.getStdOut().writer().print("here - 1: {}\n", .{println});
            // var argsArray = [_]?*LLVM.struct_LLVMOpaqueValue{null};
            var argsArray = [1]?*LLVM.struct_LLVMOpaqueValue{null};
            try std.io.getStdOut().writer().print("here - 2: {}\n", .{&argsArray[0]});
            // const args = @as([*c]?*LLVM.struct_LLVMOpaqueValue, &argsArray);

            try std.io.getStdOut().writer().print("here - 3\n", .{});
            // _ = LLVM.LLVMBuildCall2(
            //     state.builder,
            //     LLVM.LLVMVoidType(),
            //     println,
            //     &argsArray[0],
            //     0,
            //     "",
            // );
            _ = try call(state, "_printLn", &argsArray, 1);
            try std.io.getStdOut().writer().print("here - 4\n", .{});

            // _ = println;

            return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0);
        },
        // .println => {
        //     const printf = LLVM.LLVMGetNamedFunction(state.module, "printf") orelse {
        //         try std.io.getStdErr().writer().print("Error: printf not found\n", .{});
        //         std.process.exit(1);
        //     };

        //     const format = LLVM.LLVMConstString("%d\n", 3, 0);
        //     const formatPtr = LLVM.LLVMAddGlobal(state.module, LLVM.LLVMTypeOf(format), "format");
        //     LLVM.LLVMSetInitializer(formatPtr, format);
        //     LLVM.LLVMSetLinkage(formatPtr, LLVM.LLVMInternalLinkage);

        //     const value = try compileExpr(ast.kind.println.exprs[0], state);
        //     const valuePtr = LLVM.LLVMBuildAlloca(state.builder, LLVM.LLVMInt32Type(), "value");
        //     _ = LLVM.LLVMBuildStore(state.builder, value, valuePtr);

        //     var argsArray = [_]?*LLVM.struct_LLVMOpaqueValue{ formatPtr, LLVM.LLVMBuildLoad2(state.builder, LLVM.LLVMInt32Type(), valuePtr, ""), null };
        //     const args: [*c]?*LLVM.struct_LLVMOpaqueValue = @as([*c]?*LLVM.struct_LLVMOpaqueValue, &argsArray);

        //     // const args: [*c]?*LLVM.struct_LLVMOpaqueValue = [_]?*LLVM.struct_LLVMOpaqueValue{ formatPtr, LLVM.LLVMBuildLoad2(state.builder, LLVM.LLVMInt32Type(), valuePtr, "") };
        //     // var fargs = std.ArrayList(LLVM.LLVMValueRef).init(state.allocator);
        //     // defer fargs.deinit();
        //     // const ff = try fargs.toOwnedSlice();

        //     return LLVM.LLVMBuildCall2(state.builder, LLVM.LLVMInt32Type(), printf, args, 2, "");
        // },

        // else => return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0),
    }
}

pub fn call(state: *const CompilerState, name: [*c]const u8, args: [*c]LLVM.LLVMValueRef, numArgs: c_uint) !LLVM.LLVMValueRef {
    const println = LLVM.LLVMGetNamedFunction(state.module, name) orelse {
        try std.io.getStdErr().writer().print("Error: {s} not found\n", .{name});
        std.process.exit(1);
    };

    return LLVM.LLVMBuildCall2(
        state.builder,
        LLVM.LLVMVoidType(),
        println,
        args,
        numArgs,
        "",
    );
}
